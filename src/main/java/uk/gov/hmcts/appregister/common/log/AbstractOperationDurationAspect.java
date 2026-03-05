package uk.gov.hmcts.appregister.common.log;

import java.util.Arrays;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.MDC;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;

/**
 * An aspect that stores the operation name in the MDC for logging purposes. The class logs the
 * duration if required by sub classes.
 */
@Slf4j
public class AbstractOperationDurationAspect {
    /** The operation key to be used as a ubique key in the MDC. */
    public static final String OPERATION = "operation";

    /**
     * invoke the operation and store the operation name in the MDC and capture duration.
     *
     * @param startCallback Signifies the operation has been applied to the MDC so we can begin
     *     logging
     * @param afterCallback The function to call with operation name and duration when the join
     *     point has been executed
     * @param pjp The join point being executed
     * @return The object that has been returned from the join point
     */
    protected Object invokeOperationMDC(
            Consumer<String> startCallback,
            TriConsumer<String, Long, Object> afterCallback,
            ProceedingJoinPoint pjp)
            throws Throwable {
        String operation =
                pjp.getSignature().getDeclaringType().getSimpleName()
                        + "."
                        + pjp.getSignature().getName();

        // add the operation to the MDC
        MDC.put(OPERATION, operation);
        long start = System.nanoTime();

        startCallback.accept(operation);

        Object result = null;
        try {
            result = pjp.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            // call the custom function to perform some specific functionality
            afterCallback.accept(operation, durationMs, result);

            return result;
        } catch (Throwable t) {
            log.error("Exception occurred during execution", t);
            throw t;
        } finally {
            MDC.remove(OPERATION);
        }
    }

    /**
     * logs the start of the join point.
     *
     * @param proceedingJoinPoint the join point
     * @return The string to log with arguments. By default, it logs the method signature and the
     *     arguments, but it ignores any pageable arguments as they can be very large
     */
    protected String getLogStringForInputs(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedingJoinPoint.getSignature()
                + " with arguments: "
                + Arrays.toString(getIgnorePageArguments(proceedingJoinPoint));
    }

    /**
     * gets the arguments for logging, but ignores any pageable arguments as they can be very large.
     *
     * @param proceedingJoinPoint the join point
     * @return The arguments to log excluding any pageable arguments
     */
    private Object[] getIgnorePageArguments(ProceedingJoinPoint proceedingJoinPoint) {
        return Arrays.stream(
                        Arrays.stream(proceedingJoinPoint.getArgs())
                                .filter(
                                        arg ->
                                                !(arg instanceof Pageable)
                                                        && !(arg instanceof PagingWrapper))
                                // ensure that the non primitive objects are obfuscated to avoid
                                // logging PII information
                                .toArray())
                .map(
                        o -> {
                            if (isPrimitiveOrString(o)) {
                                return o;
                            } else {
                                return ObfuscationUtil.getObfuscatedString(o);
                            }
                        })
                .toArray();
    }

    public static boolean isPrimitiveOrString(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();

        return clazz.isPrimitive()
                || obj instanceof String
                || obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character;
    }

    /**
     * gets an obfuscated string for output logging.
     *
     * @param object the object to log
     * @return The obfuscated string where PII information is obfuscated.
     */
    protected String getLogStringForOutputObject(Object object) {
        return ObfuscationUtil.getObfuscatedString(object);
    }

    /** A consumer that takes three arguments. */
    public interface TriConsumer<K, V, S> {
        void accept(K k, V v, S s);
    }
}
