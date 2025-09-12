package uk.gov.hmcts.appregister.testutils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

/**
 * A global persistence class that knows how to persist objects. Specifically ones that have been
 * created using the {@link uk.gov.hmcts.appregister.testutils.data.Persistable}
 */
@Component
@RequiredArgsConstructor
public class DatabaseReset {
    private final EntityManagerFactory entityManagerFactory;

    @Autowired private final ApplicationCodeRepository applicationCodeRepository;

    @Autowired private final ApplicationListRepository applicationListRepository;

    @Autowired private final ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private final DataAuditRepository dataAuditRepository;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchema;

    /**
     * A bit crude but a sequence number that manages the data beyond all the baseline data sequence
     * numbers. This means we can manage data targeted around specific tests.
     */
    private static final int SEQUENCE_START_VALUE = 321364044;

    @Transactional
    public void resetDbData() {
        resetSequences();

        applicationListEntryRepository.deleteAll(
                applicationListEntryRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE));
        applicationCodeRepository.deleteAll(
                applicationCodeRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE));
        applicationCodeRepository.deleteAll(
                applicationCodeRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE));
        applicationListRepository.deleteAll(
                applicationListRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE));
    }

    @Transactional
    public void resetSequences() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            final Query query =
                    em.createNativeQuery(
                            "SELECT sequence_name FROM information_schema.sequences "
                                    + "WHERE sequence_schema = '"
                                    + sqlInitSchema
                                    + "'");
            final List sequences = query.getResultList();
            for (Object seqName : sequences) {
                em.createNativeQuery(
                                "ALTER SEQUENCE "
                                        + sqlInitSchema
                                        + "."
                                        + seqName
                                        + " RESTART WITH "
                                        + SEQUENCE_START_VALUE)
                        .executeUpdate();
            }
            em.getTransaction().commit();
        }
    }
}
