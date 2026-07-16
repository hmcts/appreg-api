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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
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
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * Async job lifecycle that validates and persists bulk-uploaded application entry rows for a single
 * list.
 */
@Slf4j
@RequiredArgsConstructor
public class BulkUploadAsyncLifecycle implements AsyncJobLifecycle<BulkUploadRow> {
    private static final int FIRST_DATA_ROW_NUMBER = 2;
    private static final String APPLICATION_TEXT_COLUMNS = "APPLICATION_TEXT";
    private static final String RESPONDENT_COLUMNS = "RESP_NAME_ORG/RESP_FORENAME1/RESP_SURNAME";
    private static final String BULK_UPLOAD_ROW = "BULK_UPLOAD_ROW";
    private static final Map<ErrorCodeEnum, String> BUSINESS_RULE_LOCATIONS =
            Map.ofEntries(
                    Map.entry(
                            CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                            APPLICATION_TEXT_COLUMNS),
                    Map.entry(CommonAppError.WORDING_LENGTH_FAILURE, APPLICATION_TEXT_COLUMNS),
                    Map.entry(CommonAppError.WORDING_DATA_TYPE_FAILURE, APPLICATION_TEXT_COLUMNS),
                    Map.entry(
                            AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST, "APPLICANT_CODE"),
                    Map.entry(
                            AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                            "APPLICANT_CODE"),
                    Map.entry(
                            AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST, "APPLICATION_CODE"),
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
            rowErrors.addAll(validateMappedDto(rowNumber, dto));

            if (rowErrors.isEmpty()) {
                rowErrors.addAll(validateBusinessRules(rowNumber, dto));
            }

            allErrors.addAll(rowErrors);
            rowNumber++;
        }
        nextRowNumber = rowNumber;

        addHeaderErrors(context, allErrors);
        context.setFieldCountMismatch(false);

        if (!allErrors.isEmpty()) {
            logValidationFailure(context, allErrors);
            log.error("Bulk upload validation failed with {} errors", allErrors.size());
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
        if (!context.isFieldCountMismatch() || !context.hasFailure()) {
            return;
        }

        List<BulkUploadError> errors = new ArrayList<>();
        addHeaderErrors(context, errors);
        logValidationFailure(context, errors);
        saveErrorCSV(errors, event, context);
    }

    public void setCSVFile(MultipartFile file) throws IOException {
        Path tempcsvPath = File.createTempFile(UUID.randomUUID().toString(), ".csv").toPath();
        byte[] fileBytes = file.getBytes();
        Charset charset = guessCharset(fileBytes);

        // We're copying the file over to a temp file.
        try (BufferedWriter writer = Files.newBufferedWriter(tempcsvPath, charset)) {
            writer.write(new String(file.getBytes(), charset));
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

    private List<BulkUploadError> validateMappedDto(int rowNumber, EntryCreateDto dto) {
        return beanValidator.validate(dto).stream()
                .filter(BulkUploadAsyncLifecycle::isNotWordingFieldViolation)
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> toBulkUploadError(rowNumber, violation))
                .toList();
    }

    private List<BulkUploadError> validateBusinessRules(int rowNumber, EntryCreateDto dto) {
        try {
            validationSession()
                    .validate(
                            PayloadForCreate.<EntryCreateDto>builder().id(listId).data(dto).build(),
                            (validatable, result) -> {
                                validatedRows.add(
                                        new ValidatedRow(
                                                rowNumber,
                                                dto.getWordingFields() == null
                                                        ? List.of()
                                                        : List.copyOf(dto.getWordingFields()),
                                                result));
                                return result;
                            });
            return List.of();
        } catch (AppRegistryException exception) {
            return List.of(
                    new BulkUploadError(
                            rowNumber,
                            locationForBusinessRule(exception),
                            null,
                            exception.getMessage(),
                            dto.getRespondent().getOrganisation() != null
                                    ? dto.getRespondent()
                                            .getOrganisation()
                                            .getContactDetails()
                                            .getAddressLine1()
                                    : dto.getRespondent()
                                            .getPerson()
                                            .getContactDetails()
                                            .getAddressLine1(),
                            getName(dto.getRespondent()),
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
                violation.getMessage(),
                violation.getRootBean().getRespondent().getOrganisation() != null
                        ? violation
                                .getRootBean()
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1()
                        : violation
                                .getRootBean()
                                .getRespondent()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1(),
                getName(violation.getRootBean().getRespondent()),
                "DATA_ERROR");
    }

    private static String rejectedValue(ConstraintViolation<?> violation) {
        Object invalidValue = violation.getInvalidValue();
        return invalidValue == null ? null : invalidValue.toString();
    }

    private static String locationForBusinessRule(AppRegistryException exception) {
        return BUSINESS_RULE_LOCATIONS.getOrDefault(exception.getCode(), BULK_UPLOAD_ROW);
    }

    private void logValidationFailure(JobContext context, List<BulkUploadError> error) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(error);
            context.logFailure(json);
            log.warn("Bulk upload validation failure for list {}: {}", listId, json);
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize bulk upload errors to JSON for list {}: {}",
                    listId,
                    e.getMessage(),
                    e);
            context.logFailure(
                    "Bulk upload validation failure for list " + listId + ": " + e.getMessage());
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
        // We can safely delete the temporary CSV file after the first processing pass, as the
        // validated rows are already stored in memory.
        if (csvFile != null && csvFile.exists()) {
            Files.delete(csvFile.getAbsoluteFile().toPath());
        }

        JobContext context = event.getContext();

        log.debug("Processing bulk-upload page for list {}", listId);

        var firstRowNumber =
                processingIndex < validatedRows.size()
                        ? validatedRows.get(processingIndex).rowNumber()
                        : FIRST_DATA_ROW_NUMBER;
        try {
            prepareValidatedPage(event.getData());
            var jobId = event.getResponse() == null ? null : event.getResponse().getJobId().getId();
            importedEntryCount += bulkImportService.persistPage(jobId, List.copyOf(validatedPage));
        } catch (Exception ex) {
            log.error("Failed to process bulk-import page starting at row {}", firstRowNumber, ex);
            context.logFailure(
                    "Processing failed for page starting at row "
                            + firstRowNumber
                            + ": "
                            + ex.getMessage());
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED, ex.getMessage());
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
            dto.setWordingFields(validatedRow.wordingFields());
            validatedPage.add(
                    new ValidatedBulkImportEntry(
                            validatedRow.rowNumber(), dto, validatedRow.validationResult()));
        }
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<BulkUploadRow> event) {
        if (processingIndex != validatedRows.size()) {
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
    }

    private long durationMs() {
        return startedNanos == 0
                ? 0
                : Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    private record ValidatedRow(
            int rowNumber,
            List<TemplateSubstitution> wordingFields,
            CreateApplicationEntryValidationSuccess validationResult) {}

    private static String getName(Respondent respondent) {
        if (respondent.getOrganisation() != null) {
            return respondent.getOrganisation().getName();
        }

        FullName fullName = respondent.getPerson().getName();
        if (fullName.getMiddleName().get() != null) {
            return "%s %s %s"
                    .formatted(
                            fullName.getFirstName(),
                            fullName.getMiddleName().get(),
                            fullName.getLastName());
        }

        return "%s %s".formatted(fullName.getFirstName(), fullName.getLastName());
    }

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
        } catch (IOException e) {
            log.error("Failed to save error CSV for list {}: {}", listId, e.getMessage(), e);
            context.logFailure(
                    "Failed to save error CSV for list " + listId + ": " + e.getMessage());
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED,
                    "Failed to save error CSV for list " + listId + ": " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Failed to save error CSV for list {}: {}", listId, e.getMessage(), e);
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED,
                    "Failed to save error CSV for list " + listId + ": " + e.getMessage());
        } finally {
            if (csvFile != null && csvFile.exists()) {
                Files.delete(csvFile.getAbsoluteFile().toPath());
            }
        }
    }

    private void handleHeader(List<BulkUploadError> errors, StringBuilder builder, String header) {
        if (errors.getFirst().getErrorType().equals("HEADER_ERROR")) {
            for (BulkUploadError bulkUploadError : errors) {
                if (bulkUploadError.getRowNumber() == -1) {
                    builder.append(header)
                            .append("|")
                            .append(bulkUploadError.getMessage())
                            .append("\n");
                }
            }
        } else {
            builder.append(header).append("|").append("\n");
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
                builder.append(line);
                for (BulkUploadError error : rowErrors) {
                    builder.append("|").append(error.getMessage());
                }
                builder.append("\n");
            } else {
                builder.append(line).append("|").append("\n");
            }
            rowCount++;
        }
    }
}
