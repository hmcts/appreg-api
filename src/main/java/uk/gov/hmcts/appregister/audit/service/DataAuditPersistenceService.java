package uk.gov.hmcts.appregister.audit.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
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
    private final EntityManager entityManager;
    private final DataAuditPersistenceProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(List<DataAudit> auditsToPersist) {
        entityManager.unwrap(Session.class).setJdbcBatchSize(properties.getBatchSize());
        dataAuditRepository.saveAll(auditsToPersist);
    }
}
