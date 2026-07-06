package uk.gov.hmcts.appregister.audit.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.annotation.NestedAudit;
import uk.gov.hmcts.appregister.audit.service.NestedAuditPersistenceManager;

/**
 * Enables buffered data audit persistence for explicitly annotated outer audit scopes.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class NestedAuditAspect {
    private final NestedAuditPersistenceManager nestedAuditPersistenceManager;

    @Around("@annotation(nestedAudit)")
    public Object bufferNestedAudits(ProceedingJoinPoint pjp, NestedAudit nestedAudit)
            throws Throwable {
        nestedAuditPersistenceManager.enter();
        try {
            return pjp.proceed();
        } finally {
            nestedAuditPersistenceManager.exit();
        }
    }
}
