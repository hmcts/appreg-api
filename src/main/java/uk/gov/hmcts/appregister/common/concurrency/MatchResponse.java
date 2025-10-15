package uk.gov.hmcts.appregister.common.concurrency;

import java.util.UUID;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.util.EtagUtil;

/**
 * The etag that will be returned with the associated operation payload.
 */
@Getter
public class MatchResponse<T> {
    private final String etag;
    private final T payload;

    private MatchResponse(String etag, T payload) {
        this.etag = etag;
        this.payload = payload;
    }

    public static <T> MatchResponse<T> of(UUID uuid, Versionable entity, T payload) {
        return new MatchResponse<>(EtagUtil.generateEtag(uuid, entity), payload);
    }
}
