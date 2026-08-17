package uk.gov.hmcts.appregister.audit.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.val;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

/**
 * Verifies the audit transaction overrides Hibernate's normal JDBC batch size.
 */
class DataAuditPersistenceServiceTest {
    @Test
    void givenAuditRows_whenPersisted_thenConfiguredJdbcBatchSizeIsUsed() {
        val dataAuditRepository = mock(DataAuditRepository.class);
        val properties = mock(DataAuditPersistenceProperties.class);
        val session = mock(Session.class);
        val entityManager =
                mock(
                        EntityManager.class,
                        invocation ->
                                "unwrap".equals(invocation.getMethod().getName())
                                        ? session
                                        : Answers.RETURNS_DEFAULTS.answer(invocation));
        val service =
                new DataAuditPersistenceService(dataAuditRepository, entityManager, properties);
        val audits = List.of(new DataAudit(), new DataAudit());
        when(properties.getBatchSize()).thenReturn(100);

        service.persist(audits);

        verify(session).setJdbcBatchSize(100);
        verify(dataAuditRepository).saveAll(audits);
    }
}
