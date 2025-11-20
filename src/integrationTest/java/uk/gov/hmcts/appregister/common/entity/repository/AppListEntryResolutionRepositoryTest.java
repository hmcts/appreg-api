package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_1;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionPrintProjection;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryUtil;

@Transactional
@Rollback
public class AppListEntryResolutionRepositoryTest extends BaseRepositoryTest {

    @Autowired private AppListEntryResolutionRepository applicationListEntryResolutionRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @PersistenceContext private EntityManager entityManager;

    @Test
    public void testFindByApplicationListUuidForPrinting() {
        // Arrange
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry entry =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 1);

        // Act
        List<ApplicationListEntryResolutionPrintProjection> results =
                applicationListEntryResolutionRepository.findByApplicationListUuidForPrinting(
                        list.getUuid());

        // Assert
        assertNotNull(results);
        Assertions.assertFalse(results.isEmpty());

        List<String> retrievedWordings =
                results.stream()
                        .map(ApplicationListEntryResolutionPrintProjection::getWording)
                        .toList();

        assertTrue(retrievedWordings.containsAll(List.of(WORDING_1, WORDING_2)));
        assertThat(retrievedWordings.size()).isEqualTo(2);
    }
}
