package uk.gov.hmcts.appregister.common.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A payload that represents both the id and the incoming payload content.
 */
@RequiredArgsConstructor
@Getter
@Builder
public class PayloadForCreate<T> {
    private final T data;
    private final UUID id;
}
