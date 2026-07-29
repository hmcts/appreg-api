package uk.gov.hmcts.appregister.audit.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

/**
 * Persists data audit rows in their own transaction so audit writes are independent of the caller
 * transaction.
 */
@Service
@RequiredArgsConstructor
public class DataAuditPersistenceService {
    private final DataAuditRepository dataAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(List<DataAudit> auditsToPersist) {
        dataAuditRepository.saveAll(auditsToPersist);
    }
}
