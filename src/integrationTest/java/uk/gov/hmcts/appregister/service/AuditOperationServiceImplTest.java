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
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.entity.TestEntity2;
import uk.gov.hmcts.appregister.entity.TestEntityAuditable;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.util.DifferenceLogAsserter;

public class AuditOperationServiceImplTest extends BaseIntegration {

    @MockitoBean private UserProvider provider;

    @Autowired private DataAuditLogger dataAuditLogger;

    @Autowired private AuditOperationService auditOperationService;

    @Test
    public void testGetApplicationEntryList() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});

        TestEntityAuditable test = new TestEntityAuditable();

        test.setCriminalJusticeArea(new CriminalJusticeTestData().someComplete());
        test.setId(123L);

        TestEntity2 listEntity2 = new TestEntity2();
        listEntity2.setName("e8");
        listEntity2.setId(3L);

        test.getEntry().add(listEntity2);
        test.getEntryStrings().addAll(List.of("teststring", "teststring2"));

        Object content =
                auditOperationService.processAudit(
                        Optional.empty(),
                        AppListAuditOperation.CREATE_APP_LIST,
                        (event) ->
                                Optional.of(
                                        AuditableResult.builder()
                                                .newEntity(Optional.of(test))
                                                .resultingValue("response")
                                                .build()),
                        dataAuditLogger);

        differenceLogAsserter.assertNoErrors();

        // make sure the processing was successful and lists and complex objects were diffed
        Assert.assertEquals("response", content);
        differenceLogAsserter.assertDifferenceOrDataAuditChange(
                DifferenceLogAsserter.getDataAuditAssertion(
                        "random_list",
                        "lst_entry",
                        "null",
                        "e8",
                        "CREATE",
                        "Create Application List"));
        differenceLogAsserter.assertDifferenceOrDataAuditChange(
                DifferenceLogAsserter.getDataAuditAssertion(
                        "random_list",
                        "lst_adr_id",
                        "null",
                        "3",
                        "CREATE",
                        "Create Application List"));
        Assert.assertEquals("response", content);
        differenceLogAsserter.assertDifferenceOrDataAuditChange(
                DifferenceLogAsserter.getDataAuditAssertion(
                        "test_entity",
                        "entry2",
                        "null",
                        "teststring",
                        "CREATE",
                        "Create Application List"));
        differenceLogAsserter.assertDifferenceOrDataAuditChange(
                DifferenceLogAsserter.getDataAuditAssertion(
                        "test_entity",
                        "entry2",
                        "null",
                        "teststring2",
                        "CREATE",
                        "Create Application List"));
        differenceLogAsserter.assertDifferenceOrDataAuditChange(
                DifferenceLogAsserter.getDataAuditAssertion(
                        "test_entity",
                        "adr_id",
                        "null",
                        "123",
                        "CREATE",
                        "Create Application List"));
    }
}
