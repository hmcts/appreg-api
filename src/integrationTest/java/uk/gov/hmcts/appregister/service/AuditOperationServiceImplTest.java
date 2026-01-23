package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.audit.listener.DataAuditLogger;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.entity.TestEntity2;
import uk.gov.hmcts.appregister.entity.TestEntityAuditable;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;

public class AuditOperationServiceImplTest extends BaseIntegration {

    @MockitoBean private UserProvider provider;

    @Autowired private DataAuditLogger dataAuditLogger;

    @Autowired private AuditOperationService auditOperationService;

    @Test
    public void testCreateApplicationEntryAudit() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});

        TestEntityAuditable test = new TestEntityAuditable();

        CriminalJusticeArea criminalJusticeTestData = new CriminalJusticeTestData().someComplete();
        criminalJusticeTestData.setId(999L);
        test.setCriminalJusticeArea(criminalJusticeTestData);
        test.setId(123L);
        test.setName("test name");
        TestEntity2 listEntity2 = new TestEntity2();
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        test.getEntry().add(listEntity2);
        test.getEntryStrings().addAll(List.of("teststring", "teststring2"));

        Object content =
                auditOperationService.processAudit(
                        AppListAuditOperation.CREATE_APP_LIST,
                        (event) ->
                                Optional.of(
                                        AuditableResult.builder()
                                                .newEntity(test)
                                                .resultingValue("response")
                                                .build()),
                        dataAuditLogger);

        differenceLogAsserter.assertNoErrors();

        // make sure the processing was successful
        Assert.assertEquals("response", content);
        differenceLogAsserter.assertDiffCount(3, true);
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "test_entity", "adr_id", null, "123", "CREATE", "Create Application List"));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "criminal_justice_area",
                        "cja_id",
                        null,
                        "999",
                        "CREATE",
                        "Create Application List"));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "test_entity",
                        "myname",
                        null,
                        "test name",
                        "CREATE",
                        "Create Application List"));
    }

    @Test
    public void testUpdateApplicationEntryAudit() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});

        TestEntityAuditable testNew = new TestEntityAuditable();

        CriminalJusticeArea criminalJusticeTestData = new CriminalJusticeTestData().someComplete();
        criminalJusticeTestData.setId(999L);
        testNew.setCriminalJusticeArea(criminalJusticeTestData);
        testNew.setId(123L);

        TestEntity2 listEntity2 = new TestEntity2();
        testNew.setName("new");

        testNew.getEntry().add(listEntity2);
        testNew.getEntryStrings().addAll(List.of("teststring", "teststring2"));

        CriminalJusticeArea criminalJusticeAreaOld = new CriminalJusticeTestData().someComplete();
        criminalJusticeAreaOld.setId(999L);

        TestEntityAuditable testOld = new TestEntityAuditable();
        testOld.setName("old");
        testOld.setId(123L);
        testOld.setCriminalJusticeArea(criminalJusticeAreaOld);

        testOld.getEntry().add(listEntity2);
        testOld.getEntryStrings().addAll(List.of("teststring", "teststring2"));

        Object content =
                auditOperationService.processAudit(
                        testOld,
                        new AuditOperation() {
                            @Override
                            public String getEventName() {
                                return "Event Name";
                            }

                            @Override
                            public CrudEnum getType() {
                                return CrudEnum.UPDATE;
                            }
                        },
                        (event) ->
                                Optional.of(
                                        AuditableResult.builder()
                                                .newEntity(testNew)
                                                .resultingValue("response")
                                                .build()),
                        dataAuditLogger);

        differenceLogAsserter.assertNoErrors();

        // make sure the processing was successful
        Assert.assertEquals("response", content);
        differenceLogAsserter.assertDiffCount(3, true);
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "test_entity", "adr_id", "123", "123", "UPDATE", "Event Name"));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "criminal_justice_area", "cja_id", "999", "999", "UPDATE", "Event Name"));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "test_entity", "myname", "old", "new", "UPDATE", "Event Name"));
    }

    @Test
    public void testDeleteWithOldButNoNewApplicationEntryAudit() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});

        TestEntityAuditable test = new TestEntityAuditable();
        test.setName("My_Entity_Name");
        CriminalJusticeArea criminalJusticeTestData = new CriminalJusticeTestData().someComplete();
        criminalJusticeTestData.setId(999L);
        test.setCriminalJusticeArea(criminalJusticeTestData);
        test.setId(123L);

        TestEntity2 listEntity2 = new TestEntity2();
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        test.getEntry().add(listEntity2);
        test.getEntryStrings().addAll(List.of("teststring", "teststring2"));

        Object content =
                auditOperationService.processAudit(
                        test,
                        new AuditOperation() {
                            @Override
                            public String getEventName() {
                                return "Event Name";
                            }

                            @Override
                            public CrudEnum getType() {
                                return CrudEnum.DELETE;
                            }
                        },
                        (event) ->
                                Optional.of(
                                        AuditableResult.builder()
                                                .resultingValue("response")
                                                .build()),
                        dataAuditLogger);

        differenceLogAsserter.assertNoErrors();

        // make sure the processing was successful
        Assert.assertEquals("response", content);
        differenceLogAsserter.assertDiffCount(1, false);
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "test_entity", "myname", "My_Entity_Name", null, "DELETE", "Event Name"));
    }
}
