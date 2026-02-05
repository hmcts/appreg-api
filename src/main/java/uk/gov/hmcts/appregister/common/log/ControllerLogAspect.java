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
                (name, duration) -> {
                    log.debug("Duration of {} operation {} ms", name, duration);
                    log.info("Finished Executing {} ", getLogStringForInputs(pjp));
                },
                pjp);
    }
}
