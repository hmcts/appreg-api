package uk.gov.hmcts.appregister.common.log;

import java.util.Arrays;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.MDC;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;

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
     * @param callable The function to call with operation name and duration
     * @param pjp The join point being executed
     * @return The object that has been returned from the join point
     */
    protected Object invokeOperationMDC(BiConsumer<String, Long> callable, ProceedingJoinPoint pjp)
            throws Throwable {
        String operation =
                pjp.getSignature().getDeclaringType().getSimpleName()
                        + "."
                        + pjp.getSignature().getName();

        // add the operation to the MDC
        MDC.put(OPERATION, operation);
        long start = System.nanoTime();

        Object result = null;
        try {
            result = pjp.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            // call the custom function to perform some specific functionality
            callable.accept(operation, durationMs);

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
        return Arrays.stream(proceedingJoinPoint.getArgs())
                .filter(
                        arg ->
                                !(arg instanceof org.springframework.data.domain.Pageable)
                                        && !(arg
                                                instanceof
                                                uk.gov.hmcts.appregister.common.util.PagingWrapper))
                .toArray();
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
}
