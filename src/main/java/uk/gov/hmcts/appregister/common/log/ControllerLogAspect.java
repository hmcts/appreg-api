package uk.gov.hmcts.appregister.common.log;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * An aspect that handles all logging across the controller layer.
 */
@Aspect
@Component
@Slf4j
public class ControllerLogAspect extends AbstractOperationDurationAspect {
    private static final String JSON_CONTENT_TYPE = "application/vnd.hmcts.appreg.v1+json";

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

                        // check the response type and mime header
                        if (result instanceof ResponseEntity) {
                            List<String> headers =
                                    ((ResponseEntity<?>) result).getHeaders().get("Content-Type");

                            boolean isJson = headers != null && headers.contains(JSON_CONTENT_TYPE);

                            // we only log content if json
                            if (isJson) {
                                log.debug(
                                        "Finish: Executed and returned {}",
                                        getLogStringForOutputObject(
                                                ((ResponseEntity<?>) result).getBody()));
                            } else {
                                log.debug(
                                        "Finish: Executed. Not logging response as it is not Json");
                            }
                        } else {
                            log.debug(
                                    "Finish: Executed. Not logging response as it is not a ResponseEntity or Json");
                        }
                    } else {
                        log.debug("Finish: Executed and returned null");
                    }
                },
                pjp);
    }
}
