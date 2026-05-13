package uk.gov.hmcts.appregister.testutils.annotation;

import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * A junit test invoker. It runs before each test and tells the junit framework to repeat each test
 * the specified number of times.
 *
 * <p>This classes main purpose in life is to ensure stability across a multitude of test runs. This
 * annotation would typically be used for GET endpoints to ensure that the data returned is
 * consistent across multiple invocations.
 *
 * <p>This class acts the same as the native junit RepeatedTest annotation but can also be applied
 * at the class level. Also this test invocation has a minimum fixed repeat count for all tests in
 * the class to ensure a minimum level of stability across the service.
 */
public class StabilityTestExtension implements TestTemplateInvocationContextProvider {
    static final String STABILITY_RUNS_ENV = "STABILITY_RUNS";

    private final Function<String, String> environmentLookup;

    public StabilityTestExtension() {
        this(System::getenv);
    }

    StabilityTestExtension(Function<String, String> environmentLookup) {
        this.environmentLookup = environmentLookup;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return isAnnotated(context.getTestMethod(), StabilityTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        Integer overrideTimes = getOverrideTimes();
        int times = overrideTimes != null ? overrideTimes : getTimesToRepeat(context);

        // check that the number of times we execute is at least the default
        if (overrideTimes == null && times < StabilityTest.DEFAULT_TIMES) {
            throw new AssertionFailure(
                    "To ensure stability we need to run at least %s times"
                            .formatted(StabilityTest.DEFAULT_TIMES));
        }

        return IntStream.rangeClosed(1, times)
                .mapToObj(
                        i ->
                                new TestTemplateInvocationContext() {
                                    @Override
                                    public String getDisplayName(int invocationIndex) {
                                        return "Stability Repetition " + i;
                                    }
                                });
    }

    /**
     * gets the number of times that the test should repeat.
     *
     * @param context The test context
     * @return The number of times to repeat the test
     */
    private int getTimesToRepeat(ExtensionContext context) {
        Class<?> clazz = context.getRequiredTestClass();
        Method method = context.getRequiredTestMethod();
        StabilityTest ann = clazz.getDeclaredAnnotation(StabilityTest.class);
        if (ann == null) {
            ann = method.getAnnotation(StabilityTest.class);
        }
        return ann.times();
    }

    private Integer getOverrideTimes() {
        String override = environmentLookup.apply(STABILITY_RUNS_ENV);
        if (override == null || override.isBlank()) {
            return null;
        }

        try {
            int times = Integer.parseInt(override.trim());
            if (times < 1) {
                throw new AssertionFailure(
                        "Environment variable %s must be greater than zero."
                                .formatted(STABILITY_RUNS_ENV));
            }
            return times;
        } catch (NumberFormatException ex) {
            throw new AssertionFailure(
                    "Environment variable %s must be a whole number."
                            .formatted(STABILITY_RUNS_ENV));
        }
    }
}
