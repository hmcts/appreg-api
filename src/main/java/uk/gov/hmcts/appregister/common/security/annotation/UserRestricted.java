package uk.gov.hmcts.appregister.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;
import uk.gov.hmcts.appregister.common.security.RoleNames;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(RoleNames.USER_ROLE)
public @interface UserRestricted {}
