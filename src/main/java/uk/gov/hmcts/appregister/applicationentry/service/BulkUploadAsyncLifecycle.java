package uk.gov.hmcts.appregister.applicationentry.service;

import static uk.gov.hmcts.appregister.common.async.reader.CsvReader.guessCharset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow.RespondentNameState;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * Async job lifecycle that validates and persists bulk-uploaded application entry rows for a single
 * list.
 */
@Slf4j
@RequiredArgsConstructor
public class BulkUploadAsyncLifecycle implements AsyncJobLifecycle<BulkUploadRow> {

    private static final String CLIENT_FAILURE_MESSAGE =
            "Bulk upload processing failed. Contact support quoting job reference %s.";
    private static final int FIRST_DATA_ROW_NUMBER = 2;
    private static final String APPLICATION_TEXT_COLUMNS = "APPLICATION_TEXT";
    private static final String RESPONDENT_COLUMNS = "RESP_NAME_ORG/RESP_FORENAME1/RESP_SURNAME";
    private static final String BULK_UPLOAD_ROW = "BULK_UPLOAD_ROW";
    private static final Map<String, String> CONTACT_VALIDATION_MESSAGES =
            Map.of(
                    "postcode", "Provide a valid UK postcode.",
                    "mobile", "Provide a valid UK mobile number.",
                    "phone", "Provide a valid UK telephone number.");
    private static final Map<ErrorCodeEnum, String> CLIENT_SAFE_BUSINESS_RULE_LOCATIONS =
            Map.ofEntries(
                    Map.entry(
                            CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                            APPLICATION_TEXT_COLUMNS),
                    Map.entry(CommonAppError.WORDING_LENGTH_FAILURE, APPLICATION_TEXT_COLUMNS),
                    Map.entry(CommonAppError.WORDING_DATA_TYPE_FAILURE, APPLICATION_TEXT_COLUMNS),
                    Map.entry(
                            AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST,
                            "standardApplicantCode"),
                    Map.entry(
                            AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                            "standardApplicantCode"),
                    Map.entry(AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST, "applicationCode"),
                    Map.entry(
                            AppListEntryError.ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE,
                            "ACCOUNT_NUMBER"),
                    Map.entry(
                            AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                            RESPONDENT_COLUMNS),
                    Map.entry(AppListEntryError.RESPONDENT_REQUIRED, RESPONDENT_COLUMNS),
                    Map.entry(AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED, RESPONDENT_COLUMNS),
                    Map.entry(
                            AppListEntryError.RESPONDENT_OR_NUMBER_OF_RESPONDENTS_REQUIRED,
                            RESPONDENT_COLUMNS),
                    Map.entry(
                            AppListEntryError
                                    .BULK_RESPONDENT_NUMBER_AND_RESPONDENT_MUTUALLY_EXCLUSIVE,
                            RESPONDENT_COLUMNS),
                    Map.entry(
                            AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST, "APPLICATION_LIST"),
                    Map.entry(
                            AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                            "APPLICATION_LIST"));

    private final UUID listId;
    private final ApplicationList applicationList;
    private final BulkImportService bulkImportService;
    private final BulkUploadApplicationEntryValidator validator;
    private final BulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator;
    private final ApplicationListEntryMapper mapper;
    private final Validator beanValidator;
    private final List<ValidatedBulkImportEntry> validatedPage = new ArrayList<>();
    private final List<ValidatedRow> validatedRows = new ArrayList<>();
    private BulkCreateApplicationEntryValidator.Session validationSession;
    private int nextRowNumber = FIRST_DATA_ROW_NUMBER;
    private int processingIndex;
    private int importedEntryCount;
    private long startedNanos;

    @Override
    public void received(AsyncJobLifecycleEvent<BulkUploadRow> event) {
        startedNanos = System.nanoTime();
        log.info(
                "Bulk upload started listId={} jobId={}",
                listId,
                event.getResponse().getJobId().getId());
    }

    private File csvFile;

    /**
     * Validates uploaded rows before processing starts and records row-level failures in the job
     * context.
     *
     * @param event the async lifecycle event containing the parsed rows and job context
     * @throws IOException if the underlying async infrastructure surfaces an I/O failure
     */
    @Override
    public void validating(AsyncJobLifecycleEvent<BulkUploadRow> event) throws IOException {
        List<BulkUploadRow> rows = event.getData();
        JobContext context = event.getContext();
        if (rows == null || rows.isEmpty()) {
            context.logFailure("Uploaded file contains no data rows");
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_EMPTY_FILE,
                    "Uploaded file contains no data rows");
        }

        int rowNumber = nextRowNumber;

        List<BulkUploadError> allErrors = new ArrayList<>();

        for (BulkUploadRow row : rows) {
            EntryCreateDto dto = mapper.toEntryCreateDto(row);
            List<BulkUploadError> rowErrors = new ArrayList<>();

            rowErrors.addAll(validator.validateRow(rowNumber, row));
            rowErrors.addAll(validateMappedDto(rowNumber, row, dto));
            rowErrors.addAll(validateBusinessRules(rowNumber, dto));

            allErrors.addAll(rowErrors);
            rowNumber++;
        }
        nextRowNumber = rowNumber;

        addHeaderErrors(context, allErrors);
        context.setFieldCountMismatch(false);

        if (!allErrors.isEmpty()) {
            sanitiseErrorMessages(allErrors);
            logValidationFailure(event, context, allErrors);
            saveErrorCSV(allErrors, event, context);
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED,
                    "One or more rows failed validation during bulk upload");
        }

        log.debug("Validated bulk-upload page listId={} rowCount={}", listId, rows.size());
    }

    /**
     * Formats field-count mismatches that occurred after the final validation callback, including
     * mismatches where the reader could not produce any rows.
     *
     * @param event the failed lifecycle event containing the field-count mismatch
     */
    @Override
    public void failed(AsyncJobLifecycleEvent<BulkUploadRow> event) throws IOException {
        log.warn(
                "Bulk upload failed listId={} jobId={} importedEntryCount={} durationMs={}",
                listId,
                event.getResponse() == null ? null : event.getResponse().getJobId().getId(),
                importedEntryCount,
                durationMs());

        JobContext context = event.getContext();
        if (!context.hasFailure()) {
            context.logFailure(clientFailureMessage(event));
            deleteCsvFile();
            return;
        }
        if (!context.isFieldCountMismatch()) {
            deleteCsvFile();
            return;
        }

        List<BulkUploadError> errors = new ArrayList<>();
        addHeaderErrors(context, errors);
        sanitiseErrorMessages(errors);
        logValidationFailure(event, context, errors);
        saveErrorCSV(errors, event, context);
    }

    public void setCSVFile(MultipartFile file) throws IOException {
        Path tempcsvPath = AppRegTempFileUtil.generateTempFile("bulk-upload").toPath();
        byte[] fileBytes = file.getBytes();
        Charset charset = guessCharset(fileBytes);

        // We're copying the file over to a temp file.
        try (BufferedWriter writer = Files.newBufferedWriter(tempcsvPath, charset)) {
            writer.write(new String(fileBytes, charset));
            csvFile = new File(tempcsvPath.toString());
        }
    }

    private static void addHeaderErrors(JobContext context, List<BulkUploadError> errors) {
        for (String message : context.getValidationFailureMessages()) {
            errors.addFirst(
                    new BulkUploadError(
                            -1, BULK_UPLOAD_ROW, null, message, null, null, "HEADER_ERROR"));
        }
        context.setValidationFailureMessages(new ArrayList<>());
    }

    /**
     * Runs Jakarta Bean Validation against the DTO mapped from a CSV row. This validates generated
     * OpenAPI constraints such as required values, sizes and patterns. Application-specific rules,
     * including partial respondent details and organisation/person mutual exclusion, are handled by
     * {@link BulkUploadApplicationEntryValidator} because they depend on the original CSV fields;
     * the mapper may select organisation when both respondent types are supplied, so the mapped DTO
     * no longer preserves enough information to evaluate those rules.
     */
    private List<BulkUploadError> validateMappedDto(
            int rowNumber, BulkUploadRow row, EntryCreateDto dto) {
        RespondentNameState respondentNameState = BulkUploadRow.respondentNameState(row);

        return beanValidator.validate(dto).stream()
                .filter(BulkUploadAsyncLifecycle::isNotWordingFieldViolation)
                .filter(violation -> isRelevantRespondentViolation(violation, respondentNameState))
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> toBulkUploadError(rowNumber, violation))
                .distinct()
                .toList();
    }

    private static boolean isRelevantRespondentViolation(
            ConstraintViolation<EntryCreateDto> violation,
            RespondentNameState respondentNameState) {
        // When respondent name fields have been supplied, retain their constraint violations so
        // the user can identify and correct the relevant field.
        if (respondentNameState != RespondentNameState.MISSING) {
            return true;
        }

        // The mapper creates an empty person when non-name respondent fields are supplied. Suppress
        // the resulting name constraints because the row validator already reports one actionable
        // partial-respondent error.
        return !violation.getPropertyPath().toString().startsWith("respondent.person.name");
    }

    private List<BulkUploadError> validateBusinessRules(int rowNumber, EntryCreateDto dto) {
        try {
            validationSession()
                    .validate(
                            PayloadForCreate.<EntryCreateDto>builder().id(listId).data(dto).build(),
                            (validatable, result) -> {
                                var wordingFields =
                                        dto.getWordingFields() == null
                                                ? List.<TemplateSubstitution>of()
                                                : List.copyOf(dto.getWordingFields());
                                var substitutedWording =
                                        result.getWordingSentence()
                                                .substitute(wordingFields)
                                                .getSubstitutedString();
                                validatedRows.add(
                                        new ValidatedRow(rowNumber, result, substitutedWording));
                                return result;
                            });
            return List.of();
        } catch (AppRegistryException exception) {
            if (!CLIENT_SAFE_BUSINESS_RULE_LOCATIONS.containsKey(exception.getCode())) {
                throw exception;
            }
            return List.of(
                    new BulkUploadError(
                            rowNumber,
                            locationForBusinessRule(exception),
                            null,
                            exception.getMessage(),
                            respondentAddressLine1(dto),
                            dto.getStandardApplicantCode(),
                            "DATA_ERROR"));
        }
    }

    private BulkCreateApplicationEntryValidator.Session validationSession() {
        if (validationSession == null) {
            validationSession = bulkCreateApplicationEntryValidator.createSession(applicationList);
        }
        return validationSession;
    }

    private static boolean isNotWordingFieldViolation(
            ConstraintViolation<EntryCreateDto> violation) {
        return !violation.getPropertyPath().toString().startsWith("wordingFields");
    }

    private static BulkUploadError toBulkUploadError(
            int rowNumber, ConstraintViolation<EntryCreateDto> violation) {

        return new BulkUploadError(
                rowNumber,
                violation.getPropertyPath().toString(),
                rejectedValue(violation),
                validationMessage(violation),
                respondentAddressLine1(violation.getRootBean()),
                violation.getRootBean().getStandardApplicantCode(),
                "DATA_ERROR");
    }

    private static String respondentAddressLine1(EntryCreateDto dto) {
        if (dto.getRespondent() == null) {
            return null;
        }
        if (dto.getRespondent().getOrganisation() != null) {
            return dto.getRespondent().getOrganisation().getContactDetails().getAddressLine1();
        }
        if (dto.getRespondent().getPerson() != null) {
            return dto.getRespondent().getPerson().getContactDetails().getAddressLine1();
        }
        return null;
    }

    private static String validationMessage(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
        return CONTACT_VALIDATION_MESSAGES.getOrDefault(fieldName, violation.getMessage());
    }

    private static String rejectedValue(ConstraintViolation<?> violation) {
        Object invalidValue = violation.getInvalidValue();
        return invalidValue == null ? null : invalidValue.toString();
    }

    private static String locationForBusinessRule(AppRegistryException exception) {
        return CLIENT_SAFE_BUSINESS_RULE_LOCATIONS.getOrDefault(
                exception.getCode(), BULK_UPLOAD_ROW);
    }

    private void logValidationFailure(
            AsyncJobLifecycleEvent<BulkUploadRow> event,
            JobContext context,
            List<BulkUploadError> errors) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(errors);
            context.logFailure(json);
            log.warn(
                    "Bulk upload validation failed listId={} jobId={} errorCount={}",
                    listId,
                    event.getResponse().getJobId().getId(),
                    errors.size());
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize bulk upload errors to JSON for list {}: {}",
                    listId,
                    e.getMessage(),
                    e);
            context.logFailure(clientFailureMessage(event));
        }
    }

    /**
     * Creates application entries for each validated upload row and fails the job atomically on the
     * first error.
     *
     * @param event the async lifecycle event containing the parsed rows and job context
     * @throws IOException if the underlying async infrastructure surfaces an I/O failure
     */
    @Override
    public void processing(AsyncJobLifecycleEvent<BulkUploadRow> event) throws IOException {
        JobContext context = event.getContext();

        log.debug("Processing bulk-upload page for list {}", listId);

        var jobId = event.getResponse().getJobId().getId();
        var firstRowNumber =
                processingIndex < validatedRows.size()
                        ? validatedRows.get(processingIndex).rowNumber()
                        : FIRST_DATA_ROW_NUMBER;
        try {
            prepareValidatedPage(event.getData());
            importedEntryCount += bulkImportService.persistPage(jobId, List.copyOf(validatedPage));
        } catch (Exception ex) {
            log.error(
                    "Failed to process bulk-import page listId={} jobId={} firstRowNumber={}",
                    listId,
                    jobId,
                    firstRowNumber,
                    ex);
            var clientFailureMessage = clientFailureMessage(event);
            context.logFailure(clientFailureMessage);
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED, clientFailureMessage, ex);
        } finally {
            validatedPage.clear();
        }
        log.debug(
                "Bulk upload page processed listId={} importedEntryCount={}",
                listId,
                importedEntryCount);
    }

    private void prepareValidatedPage(List<BulkUploadRow> rows) {
        validatedPage.clear();
        for (var row : rows) {
            if (processingIndex >= validatedRows.size()) {
                throw new IllegalStateException("Processing pass contains an unexpected CSV row");
            }

            var validatedRow = validatedRows.get(processingIndex++);
            var dto = mapper.toEntryCreateDto(row);
            validatedPage.add(
                    new ValidatedBulkImportEntry(
                            validatedRow.rowNumber(),
                            dto,
                            validatedRow.validationResult(),
                            validatedRow.substitutedWording()));
        }
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<BulkUploadRow> event) {
        if (processingIndex != validatedRows.size()) {
            event.getContext().logFailure(clientFailureMessage(event));
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED,
                    "Processing pass did not contain every validated CSV row");
        }

        bulkImportService.completed(
                listId, event.getResponse().getJobId().getId(), importedEntryCount);
        log.info(
                "Bulk upload completed listId={} jobId={} importedEntryCount={} durationMs={}",
                listId,
                event.getResponse().getJobId().getId(),
                importedEntryCount,
                durationMs());
        validatedRows.clear();
        deleteCsvFile();
    }

    private long durationMs() {
        return startedNanos == 0
                ? 0
                : Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    private record ValidatedRow(
            int rowNumber,
            CreateApplicationEntryValidationSuccess validationResult,
            String substitutedWording) {}

    private void saveErrorCSV(
            List<BulkUploadError> errors,
            AsyncJobLifecycleEvent<BulkUploadRow> event,
            JobContext context)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {

            StringBuilder builder = new StringBuilder();
            handleHeader(errors, builder, reader.readLine());

            writeErrorCSVLine(errors, builder, reader);

            InputStream inputStream =
                    new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
            event.getResponse().write(inputStream);
        } catch (IOException | NullPointerException e) {
            log.error("Failed to save error CSV for list {}: {}", listId, e.getMessage(), e);
            var clientFailureMessage = clientFailureMessage(event);
            context.logFailure(clientFailureMessage);
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED, clientFailureMessage, e);
        } finally {
            if (csvFile != null && csvFile.exists()) {
                Files.delete(csvFile.getAbsoluteFile().toPath());
            }
        }
    }

    private void handleHeader(List<BulkUploadError> errors, StringBuilder builder, String header) {
        builder.append(header);
        if (errors.getFirst().getErrorType().equals("HEADER_ERROR")) {
            for (BulkUploadError bulkUploadError : errors) {
                if (bulkUploadError.getRowNumber() == -1) {
                    builder.append("|").append(bulkUploadError.getMessage());
                }
            }
            builder.append("\n");
        } else {
            builder.append("|").append("\n");
        }
    }

    private void writeErrorCSVLine(
            List<BulkUploadError> errors, StringBuilder builder, BufferedReader reader)
            throws IOException {
        int rowCount = 2;

        String line;
        while ((line = reader.readLine()) != null) {
            int finalRowCount = rowCount;
            if (errors.stream().anyMatch(error -> error.getRowNumber() == finalRowCount)) {
                List<BulkUploadError> rowErrors =
                        errors.stream().filter(e -> e.getRowNumber() == finalRowCount).toList();
                rowErrors = deduplicateErrors(rowErrors);
                processErrorRows(rowErrors, builder, line);
            } else {
                builder.append(line).append("|").append("\n");
            }
            rowCount++;
        }
    }

    private List<BulkUploadError> deduplicateErrors(List<BulkUploadError> errors) {
        List<BulkUploadError> dedupedErrors = new ArrayList<>(errors);
        for (var error : errors) {
            List<BulkUploadError> dupes =
                    errors.stream()
                            .filter(
                                    e ->
                                            e.getRowNumber() == error.getRowNumber()
                                                    && e.getLocation()
                                                            .contains(error.getLocation()))
                            .toList();
            if (dupes.size() > 1) {
                dedupedErrors.removeAll(dupes.subList(1, dupes.size()));
            }
        }
        return dedupedErrors;
    }

    private void processErrorRows(
            List<BulkUploadError> errors, StringBuilder builder, String line) {
        builder.append(line);
        for (BulkUploadError error : errors) {

            if (Objects.nonNull(error.getRejectedValue()) && !error.getRejectedValue().isBlank()) {
                builder.append("|")
                        .append(
                                "%s - %s: %s"
                                        .formatted(
                                                error.getLocation(),
                                                error.getRejectedValue(),
                                                error.getMessage().contains("must match \"")
                                                        ? "Field has been rejected"
                                                        : error.getMessage()));

            } else {
                builder.append("|")
                        .append("%s: %s".formatted(error.getLocation(), error.getMessage()));
            }
        }
        builder.append("\n");
    }

    private void sanitiseErrorMessages(List<BulkUploadError> errorRows) {
        errorRows.forEach(
                error -> {

                    // This is to tidy up the location field.
                    var location =
                            error.getLocation().split("\\.").length > 1
                                    ? error.getLocation()
                                            .split("\\.")[
                                            error.getLocation().split("\\.").length - 1]
                                    : error.getLocation();

                    error.setLocation(location);

                    // removing regex from error message
                    if (error.getMessage().contains("must match \"")) {
                        error.setMessage("Field has been rejected");
                    }
                });
    }

    private static String clientFailureMessage(AsyncJobLifecycleEvent<?> event) {
        return CLIENT_FAILURE_MESSAGE.formatted(event.getResponse().getJobId().getId());
    }

    private void deleteCsvFile() {
        if (csvFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(csvFile.getAbsoluteFile().toPath());
        } catch (IOException exception) {
            log.warn("Failed to delete bulk-upload temporary CSV for list {}", listId, exception);
        }
    }
}
