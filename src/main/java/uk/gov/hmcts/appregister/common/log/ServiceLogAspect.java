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
        Object ret =
                invokeOperationMDC(
                        operation -> log.debug("Start: Executing {}", getLogStringForInputs(pjp)),
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

        return ret;
    }
}
