package uk.gov.hmcts.appregister.common.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Calculate the duration of service calls and logs it. This class takes care of logging across the
 * service layer
 */
@Aspect
@Component
@Slf4j
public class ServiceLogAspect extends AbstractOperationDurationAspect {
    @Around("(within(uk.gov.hmcts.appregister..service..*))")
    public Object logDuration(ProceedingJoinPoint pjp) throws Throwable {
        log.debug("Start: Executing {} ", getLogStringForInputs(pjp));

        Object ret =
                invokeOperationMDC(
                        (name, duration) -> {
                            log.debug("Duration of {} operation {} ms", name, duration);
                        },
                        pjp);
        log.debug("Finish: Executed {} ", getLogStringForOutputObject(ret));
        return ret;
    }
}
