package uk.gov.hmcts.appregister.controller.sort;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.controller.applicationentryresult.AbstractApplicationEntryResultCrudTest;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class GetApplicationEntryControllerSortTest extends AbstractApplicationEntryResultCrudTest {
    private static final String WEB_CONTEXT = "application-list-entries";

    @StabilityTest
    public void givenValidRequest_whenSortAccountNumber_thenReturn200() throws Exception {
        // set up the data
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry applicationListEntry = createEntry(applicationList);
        applicationListEntry.setAccountNumber("z - a account number");
        persistance.save(applicationListEntry);

        ApplicationListEntry applicationListEntry1 = createEntry(applicationList);
        applicationListEntry1.setAccountNumber("z - c account number");
        persistance.save(applicationListEntry1);

        ApplicationListEntry applicationListEntry2 = createEntry(applicationList);
        applicationListEntry2.setAccountNumber("z - b account number");
        persistance.save(applicationListEntry2);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 5;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(
                                SortableField.getSortStringForDesc(
                                        ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE)),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        // make sure the order response marries with the request data
        Assertions.assertEquals(1, page.getSort().getOrders().size());
        Assertions.assertEquals(
                SortOrdersInner.DirectionEnum.DESC,
                page.getSort().getOrders().get(0).getDirection());

        // make sure we only return defaulted externalised api sort data
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                page.getSort().getOrders().get(0).getProperty());

        // make sure the order is correct for the account number sort
        Assertions.assertEquals(applicationListEntry1.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(applicationListEntry2.getUuid(), page.getContent().get(1).getId());
        Assertions.assertEquals(applicationListEntry.getUuid(), page.getContent().get(2).getId());

        applicationListEntry = createEntry(applicationList);
        applicationListEntry.setAccountNumber("111111 - z");
        persistance.save(applicationListEntry);

        applicationListEntry1 = createEntry(applicationList);
        applicationListEntry1.setAccountNumber("111111 - c");
        persistance.save(applicationListEntry1);

        applicationListEntry2 = createEntry(applicationList);
        applicationListEntry2.setAccountNumber("111111 - b");
        persistance.save(applicationListEntry2);

        // execute the functionality with the opposite sort direction
        responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(
                                SortableField.getSortStringForAsc(
                                        ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE)),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        page = responseSpec.as(EntryPage.class);

        // make sure the order is correct for the account number sort
        Assertions.assertEquals(applicationListEntry2.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(applicationListEntry1.getUuid(), page.getContent().get(1).getId());
        Assertions.assertEquals(applicationListEntry.getUuid(), page.getContent().get(2).getId());
    }
}
