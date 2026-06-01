package uk.gov.hmcts.appregister.common.entity.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LikeAspectTest {

    private final LikeAspect aspect = new LikeAspect();

    @Test
    void givenLikeParamAnnotation_whenEscaping_thenOnlyAnnotatedStringArgumentsAreEscaped()
            throws Throwable {
        Method method =
                TestRepository.class.getDeclaredMethod(
                        "search", String.class, String.class, Integer.class);
        var pjp = joinPoint(method, "plain_%", "needs_%\\escaping", 123);

        aspect.escapeLikeParams(pjp);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.verify(pjp).proceed(argsCaptor.capture());

        Object[] args = argsCaptor.getValue();
        Assertions.assertEquals("plain_%", args[0]);
        Assertions.assertEquals("needs\\_\\%\\\\escaping", args[1]);
        Assertions.assertEquals(123, args[2]);
    }

    @Test
    void givenSameMethodInvokedAgain_whenEscaping_thenCachedMetadataIsReused() throws Throwable {
        Method method =
                TestRepository.class.getDeclaredMethod(
                        "search", String.class, String.class, Integer.class);

        aspect.escapeLikeParams(joinPoint(method, "plain", "first_%", 123));
        var pjp = joinPoint(method, "plain", "second_%", 456);

        aspect.escapeLikeParams(pjp);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.verify(pjp).proceed(argsCaptor.capture());

        Assertions.assertEquals("second\\_\\%", argsCaptor.getValue()[1]);
    }

    @Test
    void givenNoLikeParamAnnotations_whenEscaping_thenProceedWithoutReplacingArguments()
            throws Throwable {
        Method method =
                TestRepository.class.getDeclaredMethod(
                        "searchWithoutLikeParams", String.class, String.class);
        var pjp = joinPoint(method, "plain_%", "also_plain_%");

        aspect.escapeLikeParams(pjp);

        Mockito.verify(pjp).proceed();
        Mockito.verify(pjp, Mockito.never()).getArgs();
    }

    private ProceedingJoinPoint joinPoint(Method method, Object... args) throws Throwable {
        var pjp = Mockito.mock(ProceedingJoinPoint.class);
        var signature = Mockito.mock(MethodSignature.class);

        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(pjp.getSignature()).thenReturn(signature);
        Mockito.when(pjp.getArgs()).thenReturn(args);

        return pjp;
    }

    private interface TestRepository {
        void search(String plain, @LikeParam String term, Integer page);

        void searchWithoutLikeParams(String plain, String otherPlain);
    }
}
