package uk.gov.hmcts.appregister.controller.sort;

import static org.assertj.core.api.Assertions.assertThat;

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
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.controller.testutils.GetApplicationListFilterSpecification;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

/**
 * A class that isolates the sort test capabilities of the application list controller operation GET
 * application-lists.
 */
public class GetApplicationListControllerSortTest extends BaseIntegration {

    private static final String WEB_CONTEXT = "application-lists";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_CODE2 = "BCC006";

    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_CJA_CODE2 = "CE";

    private static final String OTHER_LOCATION = "Birmingham Crown Court";
    private static final String OTHER_LOCATION2 = "Cardiff Crown Court";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";

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

        createListReq.setCjaCode(VALID_CJA_CODE);
        createListReq.setOtherLocationDescription(OTHER_LOCATION);
        createListReq.setCourtLocationCode(null);

        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add a entry
        createEntry(listId);

        // add second list with different cja code
        createListReq.setCjaCode(VALID_CJA_CODE2);
        createListReq.setOtherLocationDescription(OTHER_LOCATION2);
        createListReq.setCourtLocationCode(null);

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

        // assert order
        ApplicationListPage page = createListResp.as(ApplicationListPage.class);
        Assertions.assertEquals(listId2, page.getContent().get(0).getId());
        Assertions.assertEquals(listId, page.getContent().get(1).getId());
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
        createListReq.setOtherLocationDescription(OTHER_LOCATION);
        createListReq.setCourtLocationCode(null);

        UUID listId =
                createApplicationListWithCourtCode(
                        tokenGenerator.fetchTokenForRole(), createListReq);

        // add a entry to the list
        createEntry(listId);

        // create a second list with same cja code and different location description
        createListReq.setCjaCode(VALID_CJA_CODE);
        createListReq.setOtherLocationDescription(OTHER_LOCATION2);
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

    @Test
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
                        null);

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
    @DisplayName("GET: allowed sort (date,desc & time,desc)")
    @StabilityTest
    void givenAllowedSort_thenSorted() throws Exception {

        String prefix = uniquePrefix("get-sort-allowed");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(10, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("date,desc", "time,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("B");
        assertThat(page.getContent().get(1).getDescription()).endsWith("C");
        assertThat(page.getContent().get(2).getDescription()).endsWith("A");
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
                        null);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    private UUID createApplicationListWithCourtCode(
            TokenAndJwksKey token, ApplicationListCreateDto createDetail) throws Exception {
        Response createListResp =
                restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, createDetail);
        createListResp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto createdList =
                createListResp.as(ApplicationListGetDetailDto.class);
        return createdList.getId();
    }

    private EntryGetDetailDto createEntry(UUID listId) throws Exception {
        var entryDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"), getToken(), entryDto);
        createEntryResp.then().statusCode(HttpStatus.CREATED.value());

        return createEntryResp.as(EntryGetDetailDto.class);
    }

    private ApplicationListGetDetailDto createWithCourt(
            String description, LocalDate date, LocalTime time) throws Exception {

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(date)
                        .time(time)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    // --- GET_ALL ---------------------------------------------------------------------
    private static String uniquePrefix(String base) {
        return base + " :: " + UUID.randomUUID();
    }
}
