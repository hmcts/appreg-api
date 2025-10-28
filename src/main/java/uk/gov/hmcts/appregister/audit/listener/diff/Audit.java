package uk.gov.hmcts.appregister.audit.listener.diff;

import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An audit annotation that targets a specific CRUD action.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Audit {
    /** The auditable action being performed */
    CRUDEnum action();
}
