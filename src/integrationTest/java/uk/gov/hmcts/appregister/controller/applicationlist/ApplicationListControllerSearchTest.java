package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;

public class ApplicationListControllerSearchTest extends AbstractApplicationListControllerCrudTest {

    @StabilityTest
    public void givenApplicationListSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws Exception {

        // loop through all sort fields to make sure no errors occur
        for (ApplicationListSortFieldEnum applicationEntrySortFieldEnum :
                ApplicationListSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(applicationEntrySortFieldEnum.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            responseSpec.then().statusCode(200);
            ApplicationListPage page = responseSpec.as(ApplicationListPage.class);

            // make sure the order response marries with the request data
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());

            // make sure we only return externalised api sort data
            Assertions.assertEquals(
                    applicationEntrySortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());
        }

        Assertions.assertTrue(ApplicationListSortFieldEnum.values().length > 0);
    }

    @StabilityTest
    public void
            givenApplicationListSuccessfulDefaultSort_whenSearchWithAllSortKeys_thenSuccessResponse()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationListPage page = responseSpec.as(ApplicationListPage.class);

        // make sure the order response marries with the request data
        Assertions.assertEquals(1, page.getSort().getOrders().size());
        Assertions.assertEquals(
                SortOrdersInner.DirectionEnum.ASC,
                page.getSort().getOrders().get(0).getDirection());

        // make sure we only return defaulted externalised api sort data
        Assertions.assertEquals(
                ApplicationListSortFieldEnum.DESCRIPTION.getApiValue(),
                page.getSort().getOrders().get(0).getProperty());

        Assertions.assertTrue(ApplicationListSortFieldEnum.values().length > 0);
    }

    // This test cant be made a stability test as slows the test run down
    // TODO: look into this
    @Test
    public void givenApplicationListSuccessfulSort_whenSortByEntryCount_thenSuccessResponse()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // add a list with 10 entries
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        createListReq.setCourtLocationCode(VALID_COURT_CODE);

        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // create 10 entries
        for (int i = 0; i < 10; i++) {
            // add a entry
            createEntry(listId);
        }

        createListReq.setCourtLocationCode(VALID_COURT_CODE2);

        // create a second list with 5 entries
        UUID listId2 =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // create 5 entries
        for (int i = 0; i < 5; i++) {
            // add a entry
            createEntry(listId2);
        }

        Response createListResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                ApplicationListSortFieldEnum.ENTRY_COUNT.getApiValue()
                                        + ","
                                        + "desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        GetApplicationListFilterSpecification.builder()
                                .dateValue(Optional.of(TEST_DATE.toString()))
                                .build());
        createListResp.then().statusCode(HttpStatus.OK.value());

        // assert the order of the lists
        createListResp.then().statusCode(200);
        ApplicationListPage page = createListResp.as(ApplicationListPage.class);
        Assertions.assertEquals(10, page.getContent().get(0).getEntriesCount());
        Assertions.assertEquals(listId, page.getContent().get(0).getId());
        Assertions.assertEquals(5, page.getContent().get(1).getEntriesCount());
        Assertions.assertEquals(listId2, page.getContent().get(1).getId());
        Assertions.assertTrue(ApplicationListSortFieldEnum.values().length > 0);
    }

    @StabilityTest
    public void givenApplicationListSuccessfulSort_whenSortByCourtCode_thenSuccessResponse()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // add a list with an entry
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        createListReq.setCourtLocationCode(VALID_COURT_CODE);

        // add a list with an entry
        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add a entry
        createEntry(listId);

        createListReq.setCourtLocationCode(VALID_COURT_CODE2);

        UUID listId2 =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        Response createListResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(ApplicationListSortFieldEnum.LOCATION.getApiValue() + "," + "desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        GetApplicationListFilterSpecification.builder()
                                .dateValue(Optional.of(TEST_DATE.toString()))
                                .build());
        createListResp.then().statusCode(HttpStatus.OK.value());

        // assert order is as expected
        ApplicationListPage page = createListResp.as(ApplicationListPage.class);
        Assertions.assertEquals(listId, page.getContent().get(0).getId());
        Assertions.assertEquals(listId2, page.getContent().get(1).getId());
    }

    @StabilityTest
    public void givenApplicationListSuccessfulSort_whenSortByCjaCode_thenSuccessResponse()
            throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // add initial list with cja code and location description
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add a entry
        createEntry(listId);

        // add second list with different cja code
        createListReq.setCjaCode(VALID_CJA_CODE2);
        createListReq.setOtherLocationDescription(VALID_OTHER_LOCATION);
        createListReq.setCourtLocationCode(null);

        final UUID listId2 =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add third list with different description so that is comes first in the sort list
        createListReq.setOtherLocationDescription("Alternative Location");

        final UUID listId3 =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        Response createListResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(ApplicationListSortFieldEnum.LOCATION.getApiValue() + "," + "asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        GetApplicationListFilterSpecification.builder()
                                .dateValue(Optional.of(TEST_DATE.toString()))
                                .build());
        createListResp.then().statusCode(HttpStatus.OK.value());

        // assert order
        ApplicationListPage page = createListResp.as(ApplicationListPage.class);
        Assertions.assertEquals(listId, page.getContent().get(0).getId());
        Assertions.assertEquals(listId3, page.getContent().get(1).getId());
        Assertions.assertEquals(listId2, page.getContent().get(2).getId());
    }

    @StabilityTest
    public void givenApplicationListSuccessfulSort_whenSortByCjaLocation_thenSuccessResponse()
            throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // create a list with a cja code and location description
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        createListReq.setCjaCode(VALID_CJA_CODE);
        createListReq.setOtherLocationDescription(VALID_OTHER_LOCATION);
        createListReq.setCourtLocationCode(null);

        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add a entry to the list
        createEntry(listId);

        // create a second list with same cja code and different location description
        createListReq.setCjaCode(VALID_CJA_CODE);
        createListReq.setOtherLocationDescription(VALID_OTHER_LOCATION);
        createListReq.setCourtLocationCode(null);

        UUID listId2 =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        ApplicationListGetFilterDto filterDto = new ApplicationListGetFilterDto();

        // filter on today
        filterDto.setDate(TEST_DATE);
        Response createListResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                ApplicationListSortFieldEnum.LOCATION.getApiValue()
                                        + ","
                                        + SortableField.DESC),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        GetApplicationListFilterSpecification.builder()
                                .dateValue(Optional.of(TEST_DATE.toString()))
                                .build());
        createListResp.then().statusCode(HttpStatus.OK.value());

        // assert expected order
        ApplicationListPage page = createListResp.as(ApplicationListPage.class);
        Assertions.assertEquals(listId2, page.getContent().get(0).getId());
        Assertions.assertEquals(listId, page.getContent().get(1).getId());
    }

    @DisplayName("GET: default paging + default sort (description ASC)")
    @StabilityTest
    void givenDefaults_whenGet_then200AndSortedByDescriptionAsc() throws Exception {

        String prefix = uniquePrefix("get-default-sort");

        createWithCourt(prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Alpha", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Mango", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(), // Rely on default sort
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        new OpenApiPageMetaData());

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("Alpha");
        assertThat(page.getContent().get(1).getDescription()).endsWith("Mango");
        assertThat(page.getContent().get(2).getDescription()).endsWith("Zebra");

        assertThat(page.getPageNumber()).isZero();
        assertThat(page.getPageSize()).isGreaterThanOrEqualTo(3);
        assertThat(page.getFirst()).isTrue();
    }

    @Test
    @DisplayName("GET: disallowed sort (cja) -> 400")
    void givenDisallowedSort_then400() throws Exception {

        String prefix = uniquePrefix("get-sort-disallowed");

        createWithCourt(prefix + " - X", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("cja,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        new OpenApiPageMetaData());

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void givenValidRequest_whenMultipleSortsArePresent_thenReturn400() throws Exception {

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of(
                                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                                ApplicationEntrySortFieldEnum.LOCATION.getApiValue()),
                        getLocalUrl(WEB_CONTEXT),
                        userToken);

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    @DisplayName("GET: 403 when no role")
    void givenNoRole_whenGet_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1),
                        null);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("GET: paging works (page=1,size=2)")
    void givenPaging_whenSecondPage_thenCorrectMetadata() throws Exception {

        String prefix = uniquePrefix("get-paging");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 16), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(2),
                        Optional.of(1),
                        List.of(), // default sort (description ASC)
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        new OpenApiPageMetaData());

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getElementsOnPage()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        // this check is to make sure that a default value is provided for the log when the
        // parameter is not needed.
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        null,
                        "00000000-0000-0000-0000-000000000000",
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));

        // this is to check that the description has been logged as it was included as a param.
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        prefix,
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by date + time (exact match)")
    void givenDateAndTimeFilter_thenOnlyThatSlot() throws Exception {
        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t0930 = LocalTime.of(9, 30);

        ApplicationListGetDetailDto applicationListGetDetailDto =
                createWithCourt(prefix + " - keep", day, t0930);

        // create 3 entries for the record
        createEntry(applicationListGetDetailDto.getId());
        createEntry(applicationListGetDetailDto.getId());
        createEntry(applicationListGetDetailDto.getId());

        LocalTime t1030 = LocalTime.of(10, 30);

        createWithCourt(prefix + " - drop-1", day, t1030);
        createWithCourt(prefix + " - drop-2", day.plusDays(1), t0930);

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .dateValue(Optional.of(day.toString())) // yyyy-MM-dd
                                .localTime(Optional.of("09:30"))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getDate()).isEqualTo(day);
        assertThat(only.getTime()).isEqualTo(t0930);
        assertThat(only.getDescription()).endsWith("keep");
        assertThat(only.getEntriesCount()).isEqualTo(3);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        prefix,
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        t0930.toString(),
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        null,
                        day.toString(),
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by 23:59")
    void givenTimeFilter_thenSlot() throws Exception {

        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t2359 = LocalTime.of(23, 59);

        createWithCourt(prefix + " - keep", day, t2359);

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("time", "23:59"),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getTime()).isEqualTo(t2359);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        t2359.toString(),
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by courtLocationCode")
    void givenCourtFilter_thenOnlyCourtRows() throws Exception {

        String prefix = uniquePrefix("get-court-filter");

        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .courtLocationCode(Optional.of(VALID_COURT_CODE))
                                .description(Optional.of("court-filter"))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getLocation()).isEqualTo(VALID_COURT_NAME);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        null,
                        VALID_COURT_CODE,
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by cjaCode")
    void givenCjaFilter_thenOnlyCjaRows() throws Exception {

        String prefix = uniquePrefix("get-cja-filter");

        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));
        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));

        var adminToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        adminToken,
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .cjaCode(Optional.of(VALID_CJA_CODE))
                                .otherLocationDescription(Optional.of(VALID_CJA_CODE))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getDescription()).contains(prefix);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        null,
                        VALID_CJA_CODE,
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: does not return soft deleted list")
    void givenDefaults_whenGet_then200AndNoSoftDeletedSlot() throws Exception {

        // setup a record for deletion
        String prefix = uniquePrefix("soft-deleted");
        ApplicationListGetDetailDto dto =
                createWithCourt(
                        prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        UUID id = dto.getId();

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), userToken);
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(), // Rely on default sort
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(0);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        prefix,
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET Application List")
    @StabilityTest
    void givenValidRequest_whenGetApplicationList_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getDescription()).isEqualToIgnoringCase(description);
        assertThat(dto.getCjaCode()).isEqualToIgnoringCase(VALID_CJA_CODE);
        assertThat(dto.getEntriesCount()).isEqualTo(0);
        assertThat(dto.getEntriesSummary()).isNotNull();

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        null,
                        id.toString(),
                        AppListAuditOperation.GET_APP_LIST.getType().name(),
                        AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET Application List")
    void givenValidRequest_whenGetApplicationList_then400IdFormatting() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/232322"), token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(CommonAppError.TYPE_MISMATCH_ERROR.getCode().getAppCode());
        assertThat(problemDetail.getDetail())
                .contains("Problem with value 232322 for parameter listId");
        assertThat(problemDetail.getStatus()).isEqualTo(400);

        // Making sure there is no audit log for this failed request.
        assertThatThrownBy(
                        () ->
                                differenceLogAsserter.assertDataAuditChange(
                                        DataAuditLogAsserter.getDataAuditAssertion(
                                                TableNames.APPICATION_LIST,
                                                "id",
                                                null,
                                                "232322",
                                                AppListAuditOperation.GET_APP_LIST.getType().name(),
                                                AppListAuditOperation.GET_APP_LIST.getEventName())))
                .isInstanceOf(java.lang.AssertionError.class)
                .hasMessage("We did not found expected logs");
    }

    @Test
    @DisplayName("GET Application List: 403 when no role")
    void givenNoRole_whenGetApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("GET Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenGetApplicationList_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("GET by id with full data set")
    void givenGetById_whenGet_then200AndAllDataIsReturned() throws Exception {
        // setup a record for deletion
        String prefix = uniquePrefix("soft-deleted");
        ApplicationListGetDetailDto dto =
                createWithCourt(
                        prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        UUID id = dto.getId();

        // create a single entry
        final EntryGetDetailDto entryGetDetailDto = createEntry(dto.getId());

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), userToken);

        // make the assertions on the response
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListGetDetailDto page = resp.as(ApplicationListGetDetailDto.class);

        assertThat(page.getEntriesSummary().size()).isEqualTo(1);
        Assertions.assertTrue(page.getDescription().startsWith("soft-deleted ::"));
        Assertions.assertEquals(ApplicationListStatus.OPEN, page.getStatus());
        Assertions.assertEquals(1, page.getEntriesCount());
        Assertions.assertEquals("CCC003", page.getCourtCode());
        Assertions.assertEquals("Cardiff Crown Court", page.getCourtName());
        Assertions.assertEquals(1, page.getEntriesSummary().size());
        Assertions.assertEquals(
                "Copy documents", page.getEntriesSummary().get(0).getApplicationTitle());
        Assertions.assertEquals(
                entryGetDetailDto.getAccountNumber(),
                page.getEntriesSummary().get(0).getAccountNumber().get());
        Assertions.assertEquals(1, page.getEntriesSummary().get(0).getSequenceNumber());
        Assertions.assertEquals(
                entryGetDetailDto.getRespondent().getPerson().getContactDetails().getPostcode(),
                page.getEntriesSummary().get(0).getPostCode().get());
        Assertions.assertEquals(
                page.getEntriesSummary().get(0).getApplicant().get(),
                entryGetDetailDto.getApplicant().getPerson().getName().getSurname()
                        + ", "
                        + entryGetDetailDto.getApplicant().getPerson().getName().getFirstForename()
                        + ", "
                        + entryGetDetailDto.getApplicant().getPerson().getName().getTitle());

        Assertions.assertEquals(
                page.getEntriesSummary().get(0).getRespondent().get(),
                entryGetDetailDto.getRespondent().getPerson().getName().getSurname()
                        + ", "
                        + entryGetDetailDto.getRespondent().getPerson().getName().getFirstForename()
                        + ", "
                        + entryGetDetailDto.getRespondent().getPerson().getName().getTitle());
    }

    @Test
    public void givenEntryUpdate_whenOpeningClosedList_then400() throws Exception {
        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("update-open-closed-list"));

        // update list to closed
        var updateReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(1)
                        .durationMinutes(0);

        Response updateResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, updateReq);
        updateResp.then().statusCode(HttpStatus.OK.value());

        // attempt to update back to open
        var reopenReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.OPEN)
                        .durationHours(1)
                        .durationMinutes(0);

        Response reopenResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, reopenReq);
        reopenResp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // Assert failure is due to invalid list status for update
        ProblemDetail problemDetail = reopenResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.UPDATE_NOT_ALLOWED_ON_CLOSED_LIST.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName(
            "GET Application List: entriesSummary and entriesCount exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenGetApplicationList_thenDeletedEntryExcludedFromSummaryAndCount()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("get-by-id-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // sanity-check that initial GET shows two entries
        ApplicationListGetDetailDto initial = getApplicationListDetail(listId, token);
        assertThat(initial.getEntriesCount()).isEqualTo(2L);
        assertThat(initial.getEntriesSummary()).isNotNull();
        assertThat(initial.getEntriesSummary().size()).isGreaterThanOrEqualTo(2);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // GET again and assert
        ApplicationListGetDetailDto after = getApplicationListDetail(listId, token);
        assertThat(after.getEntriesCount())
                .withFailMessage("entriesCount should exclude the soft-deleted entry")
                .isEqualTo(1L);

        assertThat(after.getEntriesSummary())
                .withFailMessage("entriesSummary must be present")
                .isNotNull();

        List<UUID> returnedEntryIds =
                after.getEntriesSummary().stream()
                        .map(ApplicationListEntrySummary::getUuid)
                        .toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in entriesSummary")
                .doesNotContain(entry2.getId());

        // sanity: remaining entry should be the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
    }

    @Test
    @DisplayName("Print Application List: entries exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenPrintApplicationList_thenDeletedEntryExcludedFromPrint()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("print-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // call print endpoint
        ApplicationListGetPrintDto printDto = getApplicationListPrint(listId, token);

        assertThat(printDto.getEntries())
                .withFailMessage("entries in print output must not be null")
                .isNotNull();

        List<UUID> returnedEntryIds =
                printDto.getEntries().stream().map(EntryGetPrintDto::getId).toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in print entries")
                .doesNotContain(entry2.getId());

        // sanity: remaining printed entry should include the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
    }

    @Test
    @DisplayName("Print Application List")
    void givenValidRequest_whenPrintApplicationList_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        Response printApplicationListResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        printApplicationListResp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        ApplicationListGetPrintDto applicationListGetPrintDto =
                printApplicationListResp.as(ApplicationListGetPrintDto.class);
        assertThat(applicationListGetPrintDto.getEntries()).isNotNull();
    }

    @Test
    @DisplayName("Print Application List: 403 when no role")
    void givenNoRole_whenPrintApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("Print Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenPrintApplicationList_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }
}
