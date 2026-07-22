package uk.gov.hmcts.appregister.applicationentryresult.validator;

import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;

public record BulkApplicationEntryResultDeletionValidatedItem(
        ListEntryResultDeleteArgs args, ListEntryResultDeleteValidationSuccess validationSuccess) {}
