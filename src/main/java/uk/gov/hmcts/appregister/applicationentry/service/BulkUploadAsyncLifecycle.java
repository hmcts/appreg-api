package uk.gov.hmcts.appregister.applicationentry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Respondent;

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
    private final ApplicationEntryService applicationEntryService;
    private final BulkUploadApplicationEntryValidator validator;
    private final BulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator;
    private final ApplicationListEntryMapper mapper;
    private final Validator beanValidator;

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

        log.info("Validating bulk upload for list {}", listId);

        if (rows == null || rows.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_EMPTY_FILE,
                    "Uploaded file contains no data rows");
        }

        validateApplicationList(context);

        int rowNumber = FIRST_DATA_ROW_NUMBER;

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

        List<String> errorMessages = context.getValidationFailureMessages();

        // we're going to convert the existing message into a BulkUploadError object and log it to
        // the job context as JSON for easier parsing later
        for (String e : errorMessages) {
            BulkUploadError bulkUploadError =
                new BulkUploadError(-1, BULK_UPLOAD_ROW, null, e, null, null, "HEADER_ERROR");
            allErrors.addFirst(bulkUploadError);
        }

        // We will clear the existing errors in the context as the original errors have been added
        // above
        context.setValidationFailureMessages(new ArrayList<>());

        if (!allErrors.isEmpty()) {
            logValidationFailure(context, allErrors);
            log.error("Bulk upload validation failed with {} errors", allErrors.size());
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED,
                    "One or more rows failed validation during bulk upload");
        }

        log.info("Bulk upload validation passed");
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
            bulkCreateApplicationEntryValidator.validate(
                    PayloadForCreate.<EntryCreateDto>builder().id(listId).data(dto).build(),
                    (validatable, result) -> null);
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

    private void validateApplicationList(JobContext context) {
        try {
            bulkCreateApplicationEntryValidator.validateApplicationList(listId);
        } catch (AppRegistryException exception) {
            String failureMessage = "[APPLICATION_LIST]: %s".formatted(exception.getMessage());
            context.logFailure(failureMessage);
            log.warn(
                    "Bulk upload application list validation failure for list {}: {}",
                    listId,
                    failureMessage);
            throw exception;
        }
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
        List<BulkUploadRow> rows = event.getData();
        JobContext context = event.getContext();

        log.info("Processing bulk upload for list {}", listId);

        int rowNumber = FIRST_DATA_ROW_NUMBER;

        for (BulkUploadRow row : rows) {
            try {
                EntryCreateDto dto = mapper.toEntryCreateDto(row);

                applicationEntryService.createBulkEntry(
                        PayloadForCreate.<EntryCreateDto>builder().id(listId).data(dto).build());

            } catch (Exception ex) {
                log.error("Failed to process row {}", rowNumber, ex);

                context.logFailure(
                        "Processing failed for row " + rowNumber + ": " + ex.getMessage());

                // Atomic failure
                throw new AppRegistryException(
                        AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED, ex.getMessage());
            }

            rowNumber++;
        }

        log.info("Bulk upload completed successfully");
    }

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
}
