package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

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

        List<BulkUploadError> allErrors = new ArrayList<>();

        int rowNumber = FIRST_DATA_ROW_NUMBER;

        for (BulkUploadRow row : rows) {
            EntryCreateDto dto = mapper.toEntryCreateDto(row);
            List<BulkUploadError> rowErrors = new ArrayList<>();
            rowErrors.addAll(validator.validateRow(rowNumber, row));
            rowErrors.addAll(validateMappedDto(rowNumber, dto));

            if (rowErrors.isEmpty()) {
                rowErrors.addAll(validateBusinessRules(rowNumber, dto));
            }

            if (!rowErrors.isEmpty()) {

                for (BulkUploadError err : rowErrors) {
                    logValidationFailure(context, err);
                }

                allErrors.addAll(rowErrors);
            }

            rowNumber++;
        }

        if (!allErrors.isEmpty()) {
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
                            exception.getMessage()));
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
                violation.getMessage());
    }

    private static String rejectedValue(ConstraintViolation<?> violation) {
        Object invalidValue = violation.getInvalidValue();
        return invalidValue == null ? null : invalidValue.toString();
    }

    private static String locationForBusinessRule(AppRegistryException exception) {
        if (isWordingError(exception)) {
            return APPLICATION_TEXT_COLUMNS;
        }

        if (exception.getCode() == AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST
                || exception.getCode()
                        == AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON) {
            return "APPLICANT_CODE";
        }

        if (exception.getCode() == AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST) {
            return "APPLICATION_CODE";
        }

        if (exception.getCode() == AppListEntryError.ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE) {
            return "ACCOUNT_NUMBER";
        }

        if (isRespondentError(exception)) {
            return RESPONDENT_COLUMNS;
        }

        if (exception.getCode() == AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST
                || exception.getCode() == AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT) {
            return "APPLICATION_LIST";
        }

        return "BULK_UPLOAD_ROW";
    }

    private static boolean isWordingError(AppRegistryException exception) {
        return exception.getCode() == CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH
                || exception.getCode() == CommonAppError.WORDING_LENGTH_FAILURE
                || exception.getCode() == CommonAppError.WORDING_DATA_TYPE_FAILURE;
    }

    private static boolean isRespondentError(AppRegistryException exception) {
        return exception.getCode()
                        == AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                || exception.getCode() == AppListEntryError.RESPONDENT_REQUIRED
                || exception.getCode() == AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED
                || exception.getCode()
                        == AppListEntryError.RESPONDENT_OR_NUMBER_OF_RESPONDENTS_REQUIRED
                || exception.getCode()
                        == AppListEntryError
                                .BULK_RESPONDENT_NUMBER_AND_RESPONDENT_MUTUALLY_EXCLUSIVE;
    }

    private void logValidationFailure(JobContext context, BulkUploadError error) {
        String failureMessage = error.toString();
        context.logFailure(failureMessage);
        log.warn("Bulk upload validation failure for list {}: {}", listId, failureMessage);
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
}
