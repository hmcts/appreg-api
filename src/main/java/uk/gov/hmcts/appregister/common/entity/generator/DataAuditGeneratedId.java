package uk.gov.hmcts.appregister.common.entity.generator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Uses the audit-specific sequence block allocator for a generated identifier.
 */
@IdGeneratorType(DataAuditIdGenerator.class)
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface DataAuditGeneratedId {}
