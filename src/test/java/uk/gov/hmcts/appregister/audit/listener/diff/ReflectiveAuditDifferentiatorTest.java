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
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;

public class ReflectiveAuditDifferentiatorTest {

    @Test
    public void testOldWithoutNewDiff() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.id = 20L;

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.CREATE, test, null);
        Assertions.assertEquals(4, differenceList.size());

        Assertions.assertEquals(
                new Difference("test_entity", "adr_id", test.id.toString(), "null"),
                differenceList.get(0));

        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(2));

        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        test.resolutionWording,
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(3));
    }

    @Test
    public void testNewWithoutOldDiff() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.id = 20L;

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.CREATE, null, test);
        Assertions.assertEquals(4, differenceList.size());

        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "adr_id",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE,
                        test.id.toString()),
                differenceList.get(0));

        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE,
                        test.criminalJusticeArea.getCode()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE,
                        test.criminalJusticeArea.getDescription()),
                differenceList.get(2));

        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE,
                        test.resolutionWording),
                differenceList.get(3));
    }

    @Test
    public void testNewAndOldFailOnIdDiff() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.id = 20L;

        TestEntityAuditable test1 = new TestEntityAuditable();
        test1.id = 201L;

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        Assertions.assertThrows(
                AppRegistryException.class,
                () -> reflectiveAuditDifferentiator.diff(CrudEnum.CREATE, test, test1));
    }

    @Test
    public void testNewAndOldNull() {
        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        Assertions.assertEquals(
                0, reflectiveAuditDifferentiator.diff(CrudEnum.CREATE, null, null).size());
    }

    @Test
    public void testChangesNoDiffWithNestedParsingEnabled() {
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList list = appListTestData.someComplete();
        list.setId(20L);

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, list, list);

        Assertions.assertEquals(0, differenceList.size());
    }

    @Test
    public void testChangesCustomComplexDiff() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.id = 20L;

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        TestEntityAuditable test1 = new TestEntityAuditable();
        test1.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test1.id = 20L;

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        ;
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, test, test1);

        Assertions.assertEquals(3, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        test1.criminalJusticeArea.getCode()),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        test1.criminalJusticeArea.getDescription()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        test.getResolutionWording(),
                        "null"),
                differenceList.get(2));
    }

    @Test
    public void testChangesCustomOldNullWithNew() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, null, test);

        Assertions.assertEquals(3, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        "null",
                        test.criminalJusticeArea.getCode()),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        "null",
                        test.criminalJusticeArea.getDescription()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        "null",
                        test.getResolutionWording()),
                differenceList.get(2));
    }

    @Test
    public void testChangesDiffContainingNewWithNullComplex() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.id = 23L;
        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        TestEntityAuditable test1 = new TestEntityAuditable();
        test1.id = 23L;
        test1.resolutionWording = "3254";
        test1.name = "my_name";
        test1.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test1.criminalJusticeArea.setCode(null);

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, test, test1);

        Assertions.assertEquals(4, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        "null"),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        test1.criminalJusticeArea.getDescription()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference("test_entity", "al_entry_resolution_wording", "32", "3254"),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference("test_entity", "myname", "null", test1.name), differenceList.get(3));
    }

    @Test
    public void testChangesDiffComplexOnlyNew() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.resolutionWording = "32";
        test.name = "myname";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(CrudEnum.READ, test);
        Assertions.assertEquals(4, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        "null",
                        test.criminalJusticeArea.getCode()),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        "null",
                        test.criminalJusticeArea.getDescription()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        "null",
                        test.getResolutionWording()),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference("test_entity", "myname", "null", test.name), differenceList.get(3));
    }

    @Test
    public void testNewComplexRecursionAndCollectionRecursionOff() {
        TestEntityAuditable test = new TestEntityAuditable();
        test.resolutionWording = "32";
        test.name = "myname";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(false, false);
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(CrudEnum.READ, test);

        Assertions.assertEquals(2, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        "null",
                        test.getResolutionWording()),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference("test_entity", "myname", "null", test.name), differenceList.get(1));
    }

    @Test
    public void testNewAndOldParsingWithRecursionParsingDisabledAndInfiniteRecursionDetection() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;

        ListEntity listEntity = new ListEntity();
        listEntity.setName("ee");
        listEntity.setId(3L);

        test.entry.add(listEntity);

        TestEntityAuditable test2 = new TestEntityAuditable();
        test2.id = 123L;
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

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, test, test2);

        Assertions.assertEquals(7, differenceList.size());
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        "null"),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        "null"),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference("test_entity", "al_entry_resolution_wording", "32", "null"),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference("random_list", "lst_adr_id", "3", "6"), differenceList.get(3));
        Assertions.assertEquals(
                new Difference("random_list", "lst_entry", "ee", "ee1"), differenceList.get(4));
        Assertions.assertEquals(
                new Difference("random_list", "lst_adr_id", "null", "3"), differenceList.get(5));
        Assertions.assertEquals(
                new Difference("random_list", "lst_entry", "null", "e8"), differenceList.get(6));
    }

    @Test
    public void testNewObjectWithComplexAndBasicListWithRecursionOffForComplexObjects() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;

        ListEntity listEntity2 = new ListEntity();
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        test.entry.add(listEntity2);
        test.entryStrings.addAll(List.of("test string", "test string2"));

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(false, true);
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(CrudEnum.READ, test);

        Assertions.assertEquals(4, differenceList.size());

        Assertions.assertEquals(
                new Difference("test_entity", "adr_id", "null", "123"), differenceList.get(0));
        Assertions.assertEquals(
                new Difference("test_entity", "al_entry_resolution_wording", "null", "32"),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference("test_entity", "entry2", "null", "test string"),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference("test_entity", "entry2", "null", "test string2"),
                differenceList.get(3));
    }

    @Test
    public void testNewAndOldWithComplexAndBasicList() {
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

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, test, test1);

        Assertions.assertEquals(6, differenceList.size());

        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        test1.criminalJusticeArea.getCode()),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        test1.criminalJusticeArea.getDescription()),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        test.resolutionWording,
                        test1.resolutionWording),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference(
                        "random_list",
                        "lst_adr_id",
                        listEntity.id.toString(),
                        listEntity1.id.toString()),
                differenceList.get(3));
        Assertions.assertEquals(
                new Difference(
                        "random_list",
                        "lst_entry",
                        listEntity.name.toString(),
                        listEntity1.name.toString()),
                differenceList.get(4));
        Assertions.assertEquals(
                new Difference(
                        "test_entity", "entry2", "test string another", "test string another 2"),
                differenceList.get(5));
    }

    @Test
    public void testOldWithComplexAndBasicList() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;

        ListEntity listEntity = new ListEntity();
        listEntity.setName("e8");
        listEntity.setId(3L);

        test.entry.add(listEntity);
        test.entryStrings.addAll(List.of("test string", "test string another"));

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(true, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.READ, test, null);

        Assertions.assertEquals(8, differenceList.size());

        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "adr_id",
                        test.id.toString(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(0));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        test.criminalJusticeArea.getCode(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(1));
        Assertions.assertEquals(
                new Difference(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        test.criminalJusticeArea.getDescription(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(2));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "al_entry_resolution_wording",
                        test.resolutionWording,
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(3));
        Assertions.assertEquals(
                new Difference(
                        "random_list",
                        "lst_adr_id",
                        listEntity.id.toString(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(4));
        Assertions.assertEquals(
                new Difference(
                        "random_list",
                        "lst_entry",
                        listEntity.name.toString(),
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(5));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "entry2",
                        "test string",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(6));
        Assertions.assertEquals(
                new Difference(
                        "test_entity",
                        "entry2",
                        "test string another",
                        ReflectiveAuditDifferentiator.EMPTY_VALUE),
                differenceList.get(7));
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

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(false, true);
        List<Difference> differenceList = reflectiveAuditDifferentiator.diff(CrudEnum.DELETE, test);
        Assertions.assertEquals(1, differenceList.size());
        Assertions.assertEquals(
                new Difference("test_entity", "myname", "null", "random name"),
                differenceList.get(0));
    }

    @Test
    public void testOldAndNewAppListEntityWithSuperFields() {
        ApplicationList oldAppLst = new AppListTestData().someComplete();
        oldAppLst.setId(123L);

        ApplicationList newAppLst = new AppListTestData().someComplete();
        newAppLst.setId(123L);

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(false, true);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.DELETE, oldAppLst, newAppLst);
        Assertions.assertNotNull(findByField("changed_date", differenceList));
        Assertions.assertNotNull(findByField("changed_by", differenceList));
        Assertions.assertNotNull(
                oldAppLst.getDescription(),
                findByField("list_description", differenceList).getOldValue());
        Assertions.assertNotNull(
                newAppLst.getDescription(),
                findByField("list_description", differenceList).getNewValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationMinutes()).toString(),
                findByField("duration_minute", differenceList).getNewValue());
        Assertions.assertEquals(
                Short.valueOf(oldAppLst.getDurationMinutes()).toString(),
                findByField("duration_minute", differenceList).getOldValue());

        Assertions.assertEquals(
                Short.valueOf(newAppLst.getDurationHours()).toString(),
                findByField("duration_hour", differenceList).getNewValue());
        Assertions.assertEquals(
                Short.valueOf(oldAppLst.getDurationHours()).toString(),
                findByField("duration_hour", differenceList).getOldValue());

        Assertions.assertNotNull(
                newAppLst.getTime(),
                findByField("application_list_time", differenceList).getNewValue());
        Assertions.assertNotNull(
                oldAppLst.getTime(),
                findByField("application_list_time", differenceList).getNewValue());

        Assertions.assertNotNull(
                newAppLst.getDate(),
                findByField("application_list_date", differenceList).getNewValue());
        Assertions.assertNotNull(
                oldAppLst.getDate(),
                findByField("application_list_date", differenceList).getNewValue());
    }

    @Test
    public void testNewAppListEntityWithSuperFieldsAndRecursionOff() {
        ApplicationList newAppLst = new AppListTestData().someComplete();

        ReflectiveAuditDifferentiator reflectiveAuditDifferentiator =
                new ReflectiveAuditDifferentiator(false, false);
        List<Difference> differenceList =
                reflectiveAuditDifferentiator.diff(CrudEnum.DELETE, newAppLst);
        Assertions.assertNotNull(findByField("changed_date", differenceList));
        Assertions.assertNotNull(findByField("changed_by", differenceList));
        Assertions.assertNotNull(
                newAppLst.getDescription(),
                findByField("list_description", differenceList).getNewValue());

        Assertions.assertNotNull(
                newAppLst.getDurationMinutes(),
                findByField("duration_minute", differenceList).getNewValue());

        Assertions.assertNotNull(
                newAppLst.getDurationHours(),
                findByField("duration_hour", differenceList).getNewValue());

        Assertions.assertNotNull(
                newAppLst.getTime(),
                findByField("application_list_time", differenceList).getNewValue());

        Assertions.assertNotNull(
                newAppLst.getDate(),
                findByField("application_list_date", differenceList).getNewValue());
    }

    private Difference findByField(String fieldName, List<Difference> differences) {
        for (Difference difference : differences) {
            if (difference.getFieldName().equals(fieldName)) {
                return difference;
            }
        }
        return null;
    }

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
