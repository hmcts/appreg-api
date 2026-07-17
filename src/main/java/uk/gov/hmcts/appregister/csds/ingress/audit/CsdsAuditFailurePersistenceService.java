package uk.gov.hmcts.appregister.csds.ingress.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CsdsAuditFailurePersistenceService {
    private final CsdsAuditWriteService csdsAuditWriteService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(List<CsdsAuditEntry> auditsToPersist) {
        csdsAuditWriteService.persist(auditsToPersist);
    }
}
