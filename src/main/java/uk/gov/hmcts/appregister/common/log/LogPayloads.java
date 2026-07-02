package uk.gov.hmcts.appregister.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a controller method into payload logging for JSON request and/or response bodies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPayloads {
    String DEFAULT_REQUEST_PREFIX = "Request payload";
    String DEFAULT_RESPONSE_PREFIX = "Response payload";

    PayloadLogDirection direction() default PayloadLogDirection.BOTH;

    String requestPrefix() default DEFAULT_REQUEST_PREFIX;

    String responsePrefix() default DEFAULT_RESPONSE_PREFIX;

    PayloadLogLevel level() default PayloadLogLevel.INFO;
}
