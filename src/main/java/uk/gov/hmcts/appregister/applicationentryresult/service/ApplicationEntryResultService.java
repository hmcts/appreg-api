package uk.gov.hmcts.appregister.applicationentryresult.service;

import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;

/**
 * Service interface for managing application list entry results.
 */
public interface ApplicationEntryResultService {
    void delete(ListEntryResultDeleteArgs args);

    MatchResponse<ResultGetDto> create(
            PayloadForCreateEntryResult<ResultCreateDto> resultCreateDto);
}
