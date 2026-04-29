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
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

/**
 * Async job lifecycle that validates and persists bulk-uploaded application entry rows for a single
 * list.
 */
@Slf4j
@RequiredArgsConstructor
public class BulkUploadAsyncLifecycle implements AsyncJobLifecycle<BulkUploadRow> {

    private final UUID listId;
    private final ApplicationEntryService applicationEntryService;
    private final BulkUploadApplicationEntryValidator validator;
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

        int rowNumber = 2; // header is row 1

        for (BulkUploadRow row : rows) {
            List<BulkUploadError> rowErrors = new ArrayList<>();
            rowErrors.addAll(validator.validateRow(rowNumber, row));
            rowErrors.addAll(validateMappedDto(rowNumber, row));

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

    private List<BulkUploadError> validateMappedDto(int rowNumber, BulkUploadRow row) {
        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        return beanValidator.validate(dto).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> toBulkUploadError(rowNumber, violation))
                .toList();
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

    private void logValidationFailure(JobContext context, BulkUploadError error) {
        String failureMessage =
                "Row "
                        + error.getRowNumber()
                        + " ["
                        + error.getColumn()
                        + "]: "
                        + error.getMessage();

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

        int rowNumber = 2;

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
