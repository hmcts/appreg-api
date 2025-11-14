package uk.gov.hmcts.appregister.audit.listener.diff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * A {@link uk.gov.hmcts.appregister.common.entity.base.Keyable} based annotation that we can add to
 * a class in correspondence with {@link Audit} annotations. These annotations are to be used by the
 * {@link Auditor} abd {@link Auditable} implementation to determine differences between objects.
 *
 * <p>This annotation makes clear what audit operations the class is enabled for and then uses @link
 * Audit} to determine the exact audit fields to monitor. If this annotation is absent from a {@link
 * uk.gov.hmcts.appregister.common.entity.base.Keyable} class then all fields will be audited for/
 * all audit operations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditEnabled {
    /** The audit types that the annotation reacts to. */
    CrudEnum[] types();
}
