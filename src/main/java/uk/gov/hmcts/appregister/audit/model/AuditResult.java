package uk.gov.hmcts.appregister.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.Optional;

/**
 * The result of an audit operation containing both the old value (if applicable). Only applies on an UPDATE or DELETE and new value
 */
@Builder
@Getter
@AllArgsConstructor
public class AuditResult<R> {
    private final R resultingValue;

    /** The old entity contains the old data prior to the update or soft delete */
    private final Optional<Keyable> oldEntity;

    /** The new entity contains the updates data post update or soft delete*/
    private final Optional<Keyable> newEntity;
}
