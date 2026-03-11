package uk.gov.hmcts.appregister.common.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * An aspect that handles all logging across the controller layer.
 */
@Aspect
@Component
@Slf4j
public class ControllerLogAspect extends AbstractOperationDurationAspect {
    @Around("(within(uk.gov.hmcts.appregister..controller..*))")
    public Object logDuration(ProceedingJoinPoint pjp) throws Throwable {
        return invokeOperationMDC(
                (op) -> {
                    // Dont do anything here as we want to log
                    // the duration in the after callback only
                },
                (name, duration, result) -> {
                    log.debug("Duration of {} operation {} ms", name, duration);
                    if (result != null) {
                        log.debug(
                                "Finish: Executed and returned {}",
                                getLogStringForOutputObject(result));
                    } else {
                        log.debug("Finish: Executed and returned null");
                    }
                },
                pjp);
    }
}
