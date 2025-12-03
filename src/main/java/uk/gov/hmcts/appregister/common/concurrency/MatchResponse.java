package uk.gov.hmcts.appregister.common.concurrency;

import java.util.List;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
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

    public static <T> MatchResponse<T> of(T payload, List<Keyable> entities) {
        return new MatchResponse<>(EtagUtil.generateEtag(entities), payload);
    }
}
