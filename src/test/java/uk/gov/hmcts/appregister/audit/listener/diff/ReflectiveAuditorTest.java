package uk.gov.hmcts.appregister.audit.listener.diff;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;

public class ReflectiveAuditorTest {

    @Test
    public void testParsingWithRecursionParsingDisabledAndInfiniteRecursionDetection() {
        TestEntityAuditable test2 = new TestEntityAuditable();
        test2.id = 123L;
        test2.resolutionWording = "32";

        test2.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        ListEntity listEntitya = new ListEntity();
        listEntitya.setName("ee1");
        listEntitya.setId(6L);

        ListEntity listEntity2 = new ListEntity();

        // simulate recursion
        listEntity2.setEntity(test2);
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        test2.entry.add(listEntitya);
        test2.entry.add(listEntity2);

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(true);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.CREATE, test2);

        Assertions.assertEquals(6, differenceList.size());
        Assertions.assertEquals(
                new AuditableData("test_entity", "adr_id", test2.id.toString()),
                differenceList.get(0));
        Assertions.assertEquals(
                new AuditableData(TableNames.CRIMINAL_JUSTICE_AREA, "cja_id", ""),
                differenceList.get(1));
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test2.criminalJusticeArea.getCode()),
                differenceList.get(2));
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test2.criminalJusticeArea.getDescription()),
                differenceList.get(3));
        Assertions.assertEquals(
                new AuditableData("test_entity", "al_entry_resolution_wording", "32"),
                differenceList.get(4));
        Assertions.assertEquals(
                new AuditableData("test_entity", "myname", ""), differenceList.get(5));
    }

    @Test
    public void testWithComplexAndBasicListWithRecursionOffForComplexObjects() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.criminalJusticeArea.setId(999L);
        test.id = 123L;

        ListEntity listEntity2 = new ListEntity();
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        // lists should be ignored in audit output
        test.entry.add(listEntity2);
        test.entryStrings.addAll(List.of("test string", "test string2"));

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(false);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.READ, test);

        Assertions.assertEquals(3, differenceList.size());

        Assertions.assertEquals(
                new AuditableData("test_entity", "adr_id", test.id.toString()),
                differenceList.get(0));
        Assertions.assertEquals(
                new AuditableData("test_entity", "al_entry_resolution_wording", "32"),
                differenceList.get(1));
        Assertions.assertEquals(
                new AuditableData("test_entity", "myname", ""), differenceList.get(2));
    }

    @Test
    public void testWithComplexAndBasicList() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;

        ListEntity listEntity = new ListEntity();
        listEntity.setName("e8");
        listEntity.setId(3L);

        test.entry.add(listEntity);
        test.entryStrings.addAll(List.of("test string", "test string another"));

        TestEntityAuditable test1 = new TestEntityAuditable();

        test1.resolutionWording = "32544";
        test1.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test1.id = 123L;

        ListEntity listEntity1 = new ListEntity();
        listEntity1.setId(5L);
        listEntity1.setName("e832");

        test1.entry.add(listEntity1);
        test1.entryStrings.addAll(List.of("test string", "test string another 2"));

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(true);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.READ, test);

        Assertions.assertEquals(6, differenceList.size());

        Assertions.assertEquals(
                new AuditableData("test_entity", "adr_id", test.id.toString()),
                differenceList.get(0));
        Assertions.assertEquals(
                new AuditableData(TableNames.CRIMINAL_JUSTICE_AREA, "cja_id", ""),
                differenceList.get(1));
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode()),
                differenceList.get(2));
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription()),
                differenceList.get(3));
        Assertions.assertEquals(
                new AuditableData("test_entity", "al_entry_resolution_wording", "32"),
                differenceList.get(4));
        Assertions.assertEquals(
                new AuditableData("test_entity", "myname", ""), differenceList.get(5));
    }

    @Test
    public void testAuditForDeleteWithAnnotation() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;
        test.name = "random name";

        ListEntity listEntity2 = new ListEntity();
        listEntity2.setName("e8");
        listEntity2.setId(3L);
        listEntity2.name = "my name";

        test.entry.add(listEntity2);
        test.entryStrings.addAll(List.of("test string", "test string2"));

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(false);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.DELETE, test);
        Assertions.assertEquals(1, differenceList.size());
        Assertions.assertEquals(
                new AuditableData("test_entity", "myname", "random name"), differenceList.get(0));
    }

    @Test
    public void testWithSuperFields() {
        ApplicationList oldAppLst = new AppListTestData().someComplete();
        oldAppLst.setId(123L);

        ApplicationList newAppLst = new AppListTestData().someComplete();
        newAppLst.setId(123L);

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(false);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.DELETE, oldAppLst);
        List<AuditableData> newDifferenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.DELETE, newAppLst);

        Assertions.assertNotNull(findByField("changed_date", differenceList));
        Assertions.assertNotNull(findByField("changed_by", differenceList));
        Assertions.assertNotNull(
                oldAppLst.getDescription(),
                findByField("list_description", differenceList).getValue());
        Assertions.assertNotNull(
                newAppLst.getDescription(),
                findByField("list_description", newDifferenceList).getValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationMinutes()).toString(),
                findByField("duration_minute", newDifferenceList).getValue());
        Assertions.assertEquals(
                Short.valueOf(oldAppLst.getDurationMinutes()).toString(),
                findByField("duration_minute", differenceList).getValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationHours()).toString(),
                findByField("duration_hour", newDifferenceList).getValue());
        Assertions.assertEquals(
                Short.valueOf(oldAppLst.getDurationHours()).toString(),
                findByField("duration_hour", differenceList).getValue());

        Assertions.assertNotNull(
                newAppLst.getTime(),
                findByField("application_list_time", newDifferenceList).getValue());
        Assertions.assertNotNull(
                oldAppLst.getTime(),
                findByField("application_list_time", differenceList).getValue());

        Assertions.assertNotNull(
                newAppLst.getDate(),
                findByField("application_list_date", newDifferenceList).getValue());
        Assertions.assertNotNull(
                oldAppLst.getDate(),
                findByField("application_list_date", differenceList).getValue());
    }

    @Test
    public void testWithSuperFieldsAndRecursionOff() {
        ApplicationList newAppLst = new AppListTestData().someComplete();

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(false);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.DELETE, newAppLst);
        Assertions.assertNotNull(findByField("changed_date", differenceList));
        Assertions.assertNotNull(findByField("changed_by", differenceList));
        Assertions.assertNotNull(
                newAppLst.getDescription(),
                findByField("list_description", differenceList).getValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationMinutes()).toString(),
                findByField("duration_minute", differenceList).getValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationHours()).toString(),
                findByField("duration_hour", differenceList).getValue());

        Assertions.assertEquals(
                newAppLst.getTime().toString(),
                findByField("application_list_time", differenceList).getValue());

        Assertions.assertEquals(
                newAppLst.getDate().toString(),
                findByField("application_list_date", differenceList).getValue());
    }

    private AuditableData findByField(String fieldName, List<AuditableData> differences) {
        for (AuditableData difference : differences) {
            if (difference.getFieldName().equals(fieldName)) {
                return difference;
            }
        }
        return null;
    }

    /** Setup some test data. */
    @Getter
    @AuditEnabled(types = {CrudEnum.DELETE})
    @Table(name = "test_entity")
    class TestEntityAuditable implements Keyable {
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

        @Column(name = "myname", nullable = false)
        @Audit(action = CrudEnum.DELETE)
        private String name;

        @Column(name = "entry", nullable = false)
        @Audit(action = CrudEnum.DELETE)
        private List<ListEntity> entry = new ArrayList<>();

        @Column(name = "entry2", nullable = false)
        private List<String> entryStrings = new ArrayList<>();
    }

    @Getter
    @Setter
    @Table(name = "random_list")
    class ListEntity implements Keyable {
        @Id
        @Column(name = "lst_adr_id", nullable = false, updatable = false)
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
        @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
        @EqualsAndHashCode.Include
        private Long id;

        @Column(name = "lst_entry", nullable = false)
        @Audit(action = CrudEnum.DELETE)
        private String name;

        // test recursion
        @Column(name = "lst_entity", nullable = false)
        private TestEntityAuditable entity;
    }
}
