package uk.gov.hmcts.appregister.common.log;

import jakarta.validation.ConstraintViolationException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

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

        val signature = pjp.getSignature();
        val operation = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        val previousOperation = MDC.get(OPERATION);

        // add the operation to the MDC
        MDC.put(OPERATION, operation);
        long start = System.nanoTime();

        startCallback.accept(operation);

        Object result = null;
        try {
            result = pjp.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000L;

            // call the custom function to perform some specific functionality
            afterCallback.accept(operation, durationMs, result);

            return result;
        } catch (Throwable t) {
            if (!isExpectedRequestValidationException(t)) {
                log.error("Exception occurred during execution", t);
            }
            throw t;
        } finally {
            if (previousOperation != null) {
                MDC.put(OPERATION, previousOperation);
            } else {
                MDC.remove(OPERATION);
            }
        }
    }

    private boolean isExpectedRequestValidationException(Throwable throwable) {
        return throwable instanceof ConstraintViolationException
                || throwable instanceof MethodArgumentTypeMismatchException
                || throwable instanceof MissingServletRequestParameterException
                || throwable instanceof HttpMessageNotReadableException
                || throwable instanceof HandlerMethodValidationException
                || throwable instanceof MethodValidationException
                || isExpectedAppRegistryException(throwable);
    }

    private boolean isExpectedAppRegistryException(Throwable throwable) {
        return throwable instanceof AppRegistryException appRegistryException
                && appRegistryException.getCode().getCode().getHttpCode().is4xxClientError();
    }

    /** A consumer that takes three arguments. */
    public interface TriConsumer<K, V, S> {
        void accept(K k, V v, S s);
    }
}
