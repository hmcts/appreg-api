package uk.gov.hmcts.appregister.testutils.stubs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@SuppressWarnings({
    "PMD.ExcessiveImports", "PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
@Getter
@Slf4j
public class AppRegistryDatabaseStub {
    private final EntityManagerFactory entityManagerFactory;

    private static final int SEQUENCE_START_VALUE = 15_000;

    private static final List<String> SEQUENCES_NO_RESET = List.of(
        "revinfo_seq"
    );

    private static final List<String> SEQUENCES_RESET_FROM = List.of(
        "ac_seq",
        "adr_seq",
        "alefs_seq",
        "aleo_seq",
        "aleo_seq",
        "aler_seq",
        "al_seq",
        "ar_seq",
        "cja_seq",
        "fee_seq",
        "la_seq",
        "lcm_seq",
        "na_seq",
        "psa_seq",
        "rc_seq",
        "sa_seq"
    );


    @Transactional
    public void clearDb() {
        //TODO: Use the repositories to clear the customised data from the database
        //eventHandlerRepository.deleteAll(
          //  eventHandlerRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        //)
    }

    public void resetSequences() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            final Query query = em.createNativeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'darts'");
            final List sequences = query.getResultList();
            for (Object seqName : sequences) {
                if (SEQUENCES_RESET_FROM.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART WITH " + SEQUENCE_START_VALUE).executeUpdate();
                } else if (!SEQUENCES_NO_RESET.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART").executeUpdate();
                }
            }
            em.getTransaction().commit();
        }
    }

}
