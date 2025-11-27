package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.util.OfficialTypeUtil;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryUtil;

@Transactional
@Rollback
public class ApplicationListEntryOfficialRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ApplicationListEntryOfficialRepository applicationListEntryOfficialRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @PersistenceContext private EntityManager entityManager;

    record OfficialKey(String title, String forename, String surname, OfficialType type) {}

    @Test
    public void testFindByApplicationListUuidForPrinting() {
        // Arrange: one list with two entries
        ApplicationList list = new AppListTestData().someMinimal().build();

        ApplicationListEntry entry1 =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 1);

        ApplicationListEntry entry2 =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 2);

        // Act: bulk fetch by list UUID
        List<ApplicationListEntryOfficialPrintProjection> officials =
                applicationListEntryOfficialRepository.findByApplicationListUuidForPrinting(
                        list.getUuid(), OfficialTypeUtil.PRINTABLE_CODES);

        // Assert: non-null and only printable types returned
        assertNotNull(officials);
        assertTrue(
                officials.stream()
                        .allMatch(
                                official ->
                                        OfficialTypeUtil.PRINTABLE_CODES.contains(
                                                official.getType())),
                "Non-printable official type returned");

        // Build expected map: entryId -> set of officials (title, forename, surname, type)
        Map<Long, Set<OfficialKey>> expectedByEntry =
                Stream.of(entry1, entry2)
                        .collect(
                                Collectors.toMap(
                                        ApplicationListEntry::getId,
                                        applicationListEntry ->
                                                applicationListEntry.getOfficials().stream()
                                                        .map(
                                                                official ->
                                                                        new OfficialKey(
                                                                                official.getTitle(),
                                                                                official
                                                                                        .getForename(),
                                                                                official
                                                                                        .getSurname(),
                                                                                official
                                                                                        .getOfficialType()))
                                                        .collect(Collectors.toSet())));

        // Build actual map from projection
        Map<Long, Set<OfficialKey>> actualByEntry =
                officials.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ApplicationListEntryOfficialPrintProjection::getEntryId,
                                        Collectors.mapping(
                                                official ->
                                                        new OfficialKey(
                                                                official.getTitle(),
                                                                official.getForename(),
                                                                official.getSurname(),
                                                                official.getType()),
                                                Collectors.toSet())));

        assertEquals(
                expectedByEntry,
                actualByEntry,
                "Officials from DB should match those persisted for each entry");
    }
}
