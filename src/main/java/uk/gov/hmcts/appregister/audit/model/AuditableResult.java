package uk.gov.hmcts.appregister.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * The result of an audit operation containing both the old value (if applicable). Only applies on
 * an UPDATE or DELETE and new value
 */
@Builder
@Getter
@AllArgsConstructor
public class AuditableResult<R, E extends Keyable> {
    private final R resultingValue;

    /** The new entity contains the create, updates or soft delete. */
    private final E newEntity;
}
