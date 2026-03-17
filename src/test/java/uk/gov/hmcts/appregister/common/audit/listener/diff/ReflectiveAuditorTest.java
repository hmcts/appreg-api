package uk.gov.hmcts.appregister.common.audit.listener.diff;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Deletable;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.entity.base.TableNames;
import uk.gov.hmcts.appregister.common.entity.converter.YesNoConverter;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;

public class ReflectiveAuditorTest {

    @Test
    public void testParsingWithRecursionParsingDisabledAndInfiniteRecursionDetection() {
        TestEntityAuditable test2 = new TestEntityAuditable();
        test2.id = 123L;
        test2.resolutionWording = "32";
        test2.name = "My Name";
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

        Assertions.assertEquals(4, differenceList.size());
        Assertions.assertEquals(
                new AuditableData("test_entity", "adr_id", test2.id.toString()),
                differenceList.get(0));
        Assertions.assertEquals(
                new AuditableData(TableNames.CRIMINAL_JUSTICE_AREA, "cja_id", ""),
                differenceList.get(1));
        Assertions.assertEquals(
                new AuditableData("test_entity", "al_entry_resolution_wording", "32"),
                differenceList.get(2));
        Assertions.assertEquals(
                new AuditableData("test_entity", "myname", "My Name"), differenceList.get(3));
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
    public void testAuditForDeleteWithAnnotation() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.id = 123L;
        test.name = "random name";
        test.resolutionWording = "resolutionWording";
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
                new AuditableData(
                        "test_entity", "al_entry_resolution_wording", "resolutionWording"),
                differenceList.get(0));
    }

    @Test
    public void testWithSuperFields() {
        TestEntityAuditable test = new TestEntityAuditable();

        test.resolutionWording = "32";
        test.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        test.criminalJusticeArea.setId(100L);
        test.id = 123L;
        test.name = "random name";
        test.setChangedBy("old user");
        test.setChangedDate(OffsetDateTime.now());

        TestEntityAuditable newTest = new TestEntityAuditable();

        newTest.resolutionWording = "37";
        newTest.criminalJusticeArea = new CriminalJusticeTestData().someComplete();
        newTest.criminalJusticeArea.setId(100L);
        newTest.id = 1235L;
        newTest.name = "random name New";
        newTest.setChangedBy("new user");
        newTest.setChangedDate(OffsetDateTime.now());

        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(true);

        // no annotations for update so return all data
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.UPDATE, test);
        List<AuditableData> newDifferenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.UPDATE, newTest);

        Assertions.assertNotNull(findByField("changed_date", differenceList));
        Assertions.assertNotNull(findByField("changed_by", differenceList));

        Assertions.assertNotNull("old user", findByField("changed_by", differenceList).getValue());
        Assertions.assertNotNull(
                "new user", findByField("changed_by", newDifferenceList).getValue());
        Assertions.assertNotNull("random name", findByField("myname", differenceList).getValue());
        Assertions.assertNotNull(
                "random name New", findByField("myname", newDifferenceList).getValue());
        Assertions.assertNotNull(
                test.getCriminalJusticeArea().getId(),
                findByField("cja_id", differenceList).getValue());
        Assertions.assertNotNull(
                newTest.getCriminalJusticeArea().getCode(),
                findByField("cja_id", differenceList).getValue());
    }

    @Test
    public void testDeleteAppListAuditData() {
        ApplicationList appList = new AppListTestData().someComplete();
        appList.setId(123L);
        ReflectiveAuditor reflectiveAuditDifferentiator = new ReflectiveAuditor(true);
        List<AuditableData> differenceList =
                reflectiveAuditDifferentiator.extractAuditData(CrudEnum.DELETE, appList);

        // only id should be audited on delete
        Assertions.assertEquals(3, differenceList.size());
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.APPLICATION_LISTS, "al_id", appList.getId().toString()),
                findByField("al_id", differenceList));
        Assertions.assertEquals(
                new AuditableData(TableNames.APPLICATION_LISTS, "id", appList.getUuid().toString()),
                findByField("id", differenceList));
        Assertions.assertEquals(
                new AuditableData(
                        TableNames.APPLICATION_LISTS, "version", appList.getVersion().toString()),
                findByField("version", differenceList));
    }

    private AuditableData findByField(String fieldName, List<AuditableData> differences) {
        for (AuditableData difference : differences) {
            if (difference.getFieldName().equals(fieldName)) {
                return difference;
            }
        }
        return null;
    }

    @MappedSuperclass
    @Getter
    @Setter
    @EntityListeners(PreCreateUpdateEntityListener.class)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public class BaseEntity implements Changeable, Deletable {
        @Column(name = "changed_by", nullable = false)
        @Audit(action = {CrudEnum.UPDATE})
        private String changedBy;

        @Column(name = "changed_date", nullable = false)
        @Audit(action = {CrudEnum.UPDATE})
        private OffsetDateTime changedDate;

        @Column(name = "delete_by")
        private String deletedBy;

        @Column(name = "delete_date")
        private OffsetDateTime deletedDate;

        @Convert(converter = YesNoConverter.class)
        @Column(name = "is_deleted")
        private YesOrNo deleted;
    }

    /** Setup some test data. */
    @Getter
    @AuditEnabled(types = {CrudEnum.DELETE, CrudEnum.CREATE, CrudEnum.READ, CrudEnum.UPDATE})
    @Table(name = "test_entity")
    class TestEntityAuditable extends BaseEntity implements Keyable {
        @Id
        @Column(name = "adr_id", nullable = false, updatable = false)
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
        @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
        @EqualsAndHashCode.Include
        @Audit(action = {CrudEnum.CREATE, CrudEnum.READ})
        private Long id;

        @Column(name = "line1")
        @Size(max = 35)
        @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.UPDATE})
        private CriminalJusticeArea criminalJusticeArea;

        @Column(name = "al_entry_resolution_wording", nullable = false)
        @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
        private String resolutionWording;

        @Column(name = "myname", nullable = false)
        @Audit(action = {CrudEnum.CREATE, CrudEnum.READ, CrudEnum.UPDATE})
        private String name;

        @Column(name = "entry", nullable = false)
        @Audit(action = {CrudEnum.CREATE})
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
        @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
        private String name;

        // test recursion
        @Column(name = "lst_entity", nullable = false)
        private TestEntityAuditable entity;
    }
}
