package uk.gov.hmcts.appregister.audit.listener.diff;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Contains the auditable data that has been established around a
 * {@link uk.gov.hmcts.appregister.common.entity.base.Keyable} entity
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class AuditableData {
    private final String tableName;
    private final String fieldName;
    private final String value;
}
