package uk.gov.hmcts.appregister.audit.listener.diff;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;

import java.util.List;

public class ReflectiveAuditDifferentiatorTest {

    @Test
    public void testChangesNoDiff() {
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList list = appListTestData.someComplete();

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(list, list);

        Assertions.assertEquals(0, differenceList.size());
    }

    @Test
    public void testChangesCustomDiff() {
        TestEntity test = new TestEntity();
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();

        TestEntity test1 = new TestEntity();
        test1.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff( test1, test);

        Assertions.assertEquals(3, differenceList.size());
    }

    @Test
    public void testChangesCustomOldNull() {
        TestEntity test = new TestEntity();
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();
        test.criminalJusticeArea.setCode(null);

        TestEntity test1 = new TestEntity();
        test1.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff( test1, test);

        Assertions.assertEquals(3, differenceList.size());
    }

    @Test
    public void testChangesCustomNewNull() {
        TestEntity test = new TestEntity();
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();

        TestEntity test1 = new TestEntity();
        test1.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();
        test1.criminalJusticeArea.setCode(null);

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff( test1, test);

        Assertions.assertEquals(3, differenceList.size());
    }

    @Test
    public void testChangesDiffComplex() {
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList list = appListTestData.someComplete();

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(list);

        Assertions.assertEquals(15, differenceList.size());
        Assertions.assertEquals(TableNames.APPICATION_LIST, differenceList.get(0).getTableName());
        Assertions.assertEquals(TableNames.APPICATION_LIST, differenceList.get(0).getFieldName());

    }

    @Test
    public void testErrorDifferentIds() {
        TestEntity test = new TestEntity();
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();
        test.id = 123L;

        TestEntity test1 = new TestEntity();
        test1.id = 999L;
        test1.criminalJusticeArea = new CriminalJusticeTestData().someMinimal().build();
        test1.criminalJusticeArea.setCode(null);

        ReflectiveAuditDifferentiator  reflectiveAuditDifferentiator = new ReflectiveAuditDifferentiator();
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff( test1, test);
    }

    @Getter
    class TestEntity implements Keyable {
        @Id
        @Column(name = "adr_id", nullable = false, updatable = false)
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
        @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
        @EqualsAndHashCode.Include
        private Long id;

        @Column(name = "line1")
        @Size(max = 35)
        private CriminalJusticeArea criminalJusticeArea;

        @Column(name = "al_entry_resolution_wording", nullable = false)
        private String resolutionWording;
    }
}
