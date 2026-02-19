package uk.gov.hmcts.appregister.common.entity.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * An annotation that should be used where we require a value to be used in a LIKE SQL constructor.
 * There are certain reserved characters which this class ensures are escaped.
 */
@Aspect
@Component
public class LikeAspect {

    /**
     * Escape the parameter that has been marked with the annotation {@link LikeParam} in the
     * repository layer. This allows to escape the special characters in the like query and prevent
     * SQL injection.
     *
     * @param pjp The proceeding join point of the repository method.
     * @return The object
     */
    @Around("execution(* uk.gov.hmcts.appregister.common.entity.repository.*.*(..))")
    public Object escapeLikeParams(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();

        Annotation[][] paramAnns = method.getParameterAnnotations();
        Object[] args = pjp.getArgs();

        // find and escape the relevant value
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s && hasLikeParam(paramAnns[i])) {
                args[i] = escapeLike(s);
            }
        }
        return pjp.proceed(args);
    }

    /**
     * do the method annotations contain the {@link LikeParam} annotation.
     *
     * @param anns the method annotations
     * @return true if the annotation is present, false otherwise
     */
    private boolean hasLikeParam(Annotation[] anns) {
        for (Annotation a : anns) {
            if (a.annotationType() == LikeParam.class) {
                return true;
            }
        }

        return false;
    }

    /**
     * The escape block.
     *
     * @param raw The raw string to escape
     */
    private String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
