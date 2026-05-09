package uk.gov.hmcts.appregister.testutils.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

class StabilityTestExtensionTest {

    @Test
    void shouldUseAnnotationRepeatCountWhenOverrideIsNotSet() throws NoSuchMethodException {
        StabilityTestExtension extension = new StabilityTestExtension(name -> null);

        List<TestTemplateInvocationContext> contexts =
                extension
                        .provideTestTemplateInvocationContexts(
                                mockContext(MethodAnnotatedTest.class, "usesMethodAnnotation"))
                        .toList();

        assertEquals(7, contexts.size());
    }

    @Test
    void shouldUseEnvironmentOverrideWhenSet() throws NoSuchMethodException {
        StabilityTestExtension extension = new StabilityTestExtension(name -> "1");

        List<TestTemplateInvocationContext> contexts =
                extension
                        .provideTestTemplateInvocationContexts(
                                mockContext(MethodAnnotatedTest.class, "usesMethodAnnotation"))
                        .toList();

        assertEquals(1, contexts.size());
    }

    @Test
    void shouldRejectInvalidEnvironmentOverride() throws NoSuchMethodException {
        StabilityTestExtension extension = new StabilityTestExtension(name -> "abc");

        AssertionFailure failure =
                assertThrows(
                        AssertionFailure.class,
                        () ->
                                extension
                                        .provideTestTemplateInvocationContexts(
                                                mockContext(
                                                        MethodAnnotatedTest.class,
                                                        "usesMethodAnnotation"))
                                        .toList());

        assertEquals(
                "Environment variable STABILITY_RUNS must be a whole number.",
                failure.getMessage());
    }

    @Test
    void shouldRejectNonPositiveEnvironmentOverride() throws NoSuchMethodException {
        StabilityTestExtension extension = new StabilityTestExtension(name -> "0");

        AssertionFailure failure =
                assertThrows(
                        AssertionFailure.class,
                        () ->
                                extension
                                        .provideTestTemplateInvocationContexts(
                                                mockContext(
                                                        MethodAnnotatedTest.class,
                                                        "usesMethodAnnotation"))
                                        .toList());

        assertEquals(
                "Environment variable STABILITY_RUNS must be greater than zero.",
                failure.getMessage());
    }

    private <T> ExtensionContext mockContext(Class<T> testClass, String methodName)
            throws NoSuchMethodException {
        ExtensionContext context = mock(ExtensionContext.class);
        Method method = testClass.getDeclaredMethod(methodName);
        doReturn(testClass).when(context).getRequiredTestClass();
        when(context.getRequiredTestMethod()).thenReturn(method);
        when(context.getTestMethod()).thenReturn(Optional.of(method));
        return context;
    }

    private static final class MethodAnnotatedTest {
        @StabilityTest(times = 7)
        void usesMethodAnnotation() {
            // no-op
        }
    }
}
