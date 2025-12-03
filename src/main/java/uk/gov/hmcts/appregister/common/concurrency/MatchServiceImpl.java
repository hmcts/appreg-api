package uk.gov.hmcts.appregister.common.concurrency;

import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.EtagUtil;

/**
 * A match service that applies the match etag from the request to the etag of the entity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {
    private final MatchProvider request;

    /**
     * matches on the request etag if present. throws an exception if a match has not been found
     *
     * @param supplier The supplier to return the updated etag
     * @param entities The entities
     * @return The match response with the updated etag
     */
    public <T> MatchResponse<T> matchOnRequest(
            Supplier<MatchResponse<T>> supplier, List<Keyable> entities) {
        // Apply the match etag from the request to the entity
        if (request.getEtag() != null) {
            // Assuming the entity has a setEtag method
            String generateEtag = EtagUtil.generateEtag(entities);

            // if there is a clash between the etag in the request and the entity data then throw an
            // exception
            if (!generateEtag.equals(request.getEtag())) {
                throw new AppRegistryException(
                        CommonAppError.MATCH_ETAG_FAILURE,
                        "ETag specified %s does not match identified record"
                                .formatted(request.getEtag()));
            }
        }

        return supplier.get();
    }
}
