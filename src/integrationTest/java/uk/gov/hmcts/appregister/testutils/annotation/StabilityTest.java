package uk.gov.hmcts.appregister.testutils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This is a junit test annotation that tells a test to test stability i.e. runs multiple times
 * ensuring consistent results across the same run. This is useful for GET endpoints where data
 * consistency is important.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestMethodOrder(MethodOrderer.Random.class) // optional
@ExtendWith(StabilityTestExtension.class)
@TestTemplate
public @interface StabilityTest {
    int DEFAULT_TIMES = 10;

    /** Number of times to repeat each test. Defaults to 10 */
    int times() default DEFAULT_TIMES;
}
