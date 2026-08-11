package uk.gov.hmcts.appregister.applicationentry.service;

import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

/**
 * One CSV row together with the reference data resolved during validation.
 */
public record ValidatedBulkImportEntry(
        int rowNumber,
        EntryCreateDto entry,
        CreateApplicationEntryValidationSuccess validationResult,
        String substitutedWording) {}
