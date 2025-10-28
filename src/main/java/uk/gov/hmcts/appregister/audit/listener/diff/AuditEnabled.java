package uk.gov.hmcts.appregister.audit.listener.diff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entity based annotation that we can add to a class in correspondence with {@link Audit}
 * annotations
 * These annotations can be used by the {@link uk.gov.hmcts.appregister.audit.listener.diff.AuditDifferentiator}
 * abd {@link AuditDifferentiable} implementation to determine
 * the differences to establish
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditEnabled {
}
