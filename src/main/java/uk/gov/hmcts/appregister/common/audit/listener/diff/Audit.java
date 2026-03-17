package uk.gov.hmcts.appregister.common.audit.listener.diff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * An audit annotation that targets a specific CRUD action.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Audit {
    /** The auditable action being performed. */
    CrudEnum[] action();
}
