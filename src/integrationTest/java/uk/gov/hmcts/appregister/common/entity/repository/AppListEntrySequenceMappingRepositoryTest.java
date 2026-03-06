package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@Slf4j
@Transactional
@Rollback
public class AppListEntrySequenceMappingRepositoryTest extends BaseRepositoryTest {

    @Autowired private AppListEntrySequenceMappingRepository appListEntrySequenceMappingRepository;

    @PersistenceContext private EntityManager entityManager;

    @Test
    public void testBasicInsertionAndFetchById() {
        Long alId = createApplicationListAndGetId();

        AppListEntrySequenceMapping mapping =
                AppListEntrySequenceMapping.builder().alId(alId).aleLastSequence(123).build();

        appListEntrySequenceMappingRepository.saveAndFlush(mapping);

        AppListEntrySequenceMapping fetched =
                appListEntrySequenceMappingRepository.findById(alId).orElseThrow();

        assertEquals(alId, fetched.getAlId());
        assertEquals(123, fetched.getAleLastSequence());
    }

    @Test
    public void testUpdateAleLastSequence() {
        Long alId = createApplicationListAndGetId();

        AppListEntrySequenceMapping saved =
                appListEntrySequenceMappingRepository.saveAndFlush(
                        AppListEntrySequenceMapping.builder()
                                .alId(alId)
                                .aleLastSequence(1)
                                .build());

        saved.setAleLastSequence(2);
        appListEntrySequenceMappingRepository.saveAndFlush(saved);

        AppListEntrySequenceMapping refreshed =
                appListEntrySequenceMappingRepository.findById(alId).orElseThrow();

        assertEquals(alId, refreshed.getAlId());
        assertEquals(2, refreshed.getAleLastSequence());
    }

    private Long createApplicationListAndGetId() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        entityManager.flush();
        entityManager.refresh(list);

        assertNotNull(list.getId(), "ApplicationList DB id should be populated after save");
        return list.getId();
    }
}
