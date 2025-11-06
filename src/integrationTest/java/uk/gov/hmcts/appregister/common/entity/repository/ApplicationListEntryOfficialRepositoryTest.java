package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.projection.ApplicationListOfficialPrintProjection;
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

    record OfficialKey(String title, String forename, String surname, String type) {}

    @Test
    public void testFindByIdForPrinting() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry data =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 1);

        // test get
        List<ApplicationListOfficialPrintProjection> officials =
                applicationListEntryOfficialRepository.findByIdForPrinting(
                        data.getId(), OfficialTypeUtil.PRINTABLE_CODES);

        // assert that the data that has been retrieved aligns with the data that we
        // have stored
        assertNotNull(officials);
        assertTrue(
            officials.stream().allMatch(o ->
                                            OfficialTypeUtil.PRINTABLE_CODES.contains(o.getType())),
            "Non-printable official type returned"
        );
        var expected = data.getOfficials().stream()
            .map(o ->
                     new OfficialKey(o.getTitle(), o.getForename(), o.getSurname(), o.getOfficialType()))
            .collect(Collectors.toSet());

        var actual = officials.stream()
            .map(o ->
                     new OfficialKey(o.getTitle(), o.getForename(), o.getSurname(), o.getType()))
            .collect(Collectors.toSet());

        assertEquals(expected, actual, "Officials from DB should match those persisted");
    }
}
