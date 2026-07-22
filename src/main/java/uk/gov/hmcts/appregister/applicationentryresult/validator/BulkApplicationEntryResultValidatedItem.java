package uk.gov.hmcts.appregister.applicationentryresult.validator;

import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

public record BulkApplicationEntryResultValidatedItem(
        PayloadForCreateEntryResult<ResultCreateDto> payload,
        ListEntryResultCreateValidationSuccess validationSuccess) {}
