package uk.gov.hmcts.appregister.common.concurrency;

import java.util.List;
import java.util.function.Supplier;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

public interface MatchService {
    /**
     * matches on the request etag if present. throws an exception if a match has not been found
     *
     * @param supplier The supplier to return the updated etag
     * @param entities The versionable entity to match against
     */
    <T> MatchResponse<T> matchOnRequest(
            Supplier<MatchResponse<T>> supplier, List<Keyable> entities);
}
