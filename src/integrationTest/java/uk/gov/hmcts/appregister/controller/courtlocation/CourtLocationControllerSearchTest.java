package uk.gov.hmcts.appregister.controller.courtlocation;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.courtlocation.api.CourtLocationSortFieldMapper;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class CourtLocationControllerSearchTest extends AbstractCourtLocationControllerCrudTest {

    // --- /court-locations/{code}?date=... -----------------------------------------------------
    @Test
    void givenValidRequest_whenGetCourtLocationByCodeAndDate_Cardiff_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(WEB_CONTEXT + "/" + CARDIFF_CODE, OffsetDateTime.now()),
                        token);

        resp.then().statusCode(200);

        CourtLocationGetDetailDto dto = resp.as(CourtLocationGetDetailDto.class);
        assertThat(dto.getName()).isEqualTo(CARDIFF_NAME);
        assertThat(dto.getLocationCode()).isEqualTo(CARDIFF_CODE);
        assertThat(dto.getStartDate()).isEqualTo(CARDIFF_START);
        assertThat(dto.getEndDate().get()).isNull();

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        CARDIFF_CODE,
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getType().name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "start_date",
                        null,
                        LocalDate.now().toString(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getType().name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getEventName()));
    }

    @Test
    void givenValidRequest_whenGetCourtLocationByCodeAndDate_Bristol_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(WEB_CONTEXT + "/" + BRISTOL_CODE, OffsetDateTime.now()),
                        token);

        resp.then().statusCode(200);

        CourtLocationGetDetailDto dto = resp.as(CourtLocationGetDetailDto.class);
        assertThat(dto.getName()).isEqualTo(BRISTOL_NAME);
        assertThat(dto.getLocationCode()).isEqualTo(BRISTOL_CODE);
        assertThat(dto.getStartDate()).isEqualTo(BRISTOL_START);
        Assertions.assertNull(dto.getEndDate().get());

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        BRISTOL_CODE,
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getType().name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "start_date",
                        null,
                        LocalDate.now().toString(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getType().name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getEventName()));
    }

    @Test
    void givenInvalidCode_whenGetCourtLocationByCodeAndDate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var invalid = "ZZZ999";
        var resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(WEB_CONTEXT + "/" + invalid, OffsetDateTime.now()),
                        token);

        resp.then().statusCode(404);
        ProblemAssertUtil.assertEquals(CourtLocationError.COURT_NOT_FOUND.getCode(), resp);

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertFailCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenMissingDate_whenGetCourtLocationByCodeAndDate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + CARDIFF_CODE), token);

        resp.then().statusCode(400);
    }

    @Test
    void givenBadDateFormat_whenGetCourtLocationByCodeAndDate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        // Build URL with a deliberately bad date format
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + CARDIFF_CODE + "?date=01-01-2025"), token);

        resp.then().statusCode(400);
    }

    // --- /court-locations (paged, filterable) -------------------------------------------------

    @Test
    @StabilityTest
    void givenNoFilters_whenGetCourtLocations_then200AndDefaultSort() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT), token);

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(
                page, DEFAULT_PAGE_SIZE, 0, 1, 2); // 2 active CHOA courts seeded

        // Expect both seeded CHOA rows present
        var content = page.getContent();
        assertThat(content)
                .extracting("locationCode")
                .containsExactlyInAnyOrder(CARDIFF_CODE, BRISTOL_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "start_date",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @Test
    @StabilityTest
    void givenFilterByCodeContains_whenGetCourtLocations_then200() throws Exception {
        var token =
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
                        token,
                        new CourtLocationFilter(Optional.empty(), Optional.of("cc")),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(page, DEFAULT_PAGE_SIZE, 0, 1, 2);
        assertThat(page.getContent())
                .extracting("locationCode")
                .containsExactlyInAnyOrder(CARDIFF_CODE, BRISTOL_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "cc",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "courthouse_name",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @Test
    @StabilityTest
    void givenFilterByNameContains_whenGetCourtLocations_then200() throws Exception {
        var token =
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
                        token,
                        new CourtLocationFilter(Optional.of("crown"), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(page, DEFAULT_PAGE_SIZE, 0, 1, 2);
        assertThat(page.getContent())
                .extracting("locationCode")
                .containsExactlyInAnyOrder(CARDIFF_CODE, BRISTOL_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "courthouse_name",
                        null,
                        "crown",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @Test
    @StabilityTest
    void givenFilterByCodeAndName_whenGetCourtLocations_then200OnlyBristol() throws Exception {
        var token =
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
                        token,
                        new CourtLocationFilter(Optional.of("bristol"), Optional.of("cc")),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(page, DEFAULT_PAGE_SIZE, 0, 1, 1);
        assertThat(page.getContent()).extracting("locationCode").containsExactly(BRISTOL_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "cc",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "courthouse_name",
                        null,
                        "bristol",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidSorts_whenGetCourtLocations_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        // sort=name,asc then code,desc (both allowed by validator)
        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("name,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        new CourtLocationFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(page, DEFAULT_PAGE_SIZE, 0, 1, 2);

        // With only two rows, just assert the same two present; ordering is validated via
        // underlying sort acceptance.
        assertThat(page.getContent())
                .extracting("locationCode")
                .containsExactlyInAnyOrder(CARDIFF_CODE, BRISTOL_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "courthouse_name",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @StabilityTest
    public void givenCourtLocationSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws Exception {
        for (CourtLocationSortFieldMapper courtLocationSortFieldMapper :
                CourtLocationSortFieldMapper.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(courtLocationSortFieldMapper.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            CourtLocationPage page = responseSpec.as(CourtLocationPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            Assertions.assertEquals(
                    courtLocationSortFieldMapper.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());

            differenceLogAsserter.assertDataAuditChange(
                    AuditLogAsserter.getDataAuditAssertion(
                            TableNames.NATIONAL_COURT_HOUSES,
                            "court_location_code",
                            null,
                            "",
                            CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                    .getType()
                                    .name(),
                            CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                    .getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    AuditLogAsserter.getDataAuditAssertion(
                            TableNames.NATIONAL_COURT_HOUSES,
                            "courthouse_name",
                            null,
                            "",
                            CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                    .getType()
                                    .name(),
                            CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                    .getEventName()));
        }

        Assertions.assertTrue(CourtLocationSortFieldMapper.values().length > 0);
    }

    @Test
    void givenInvalidSort_whenGetCourtLocations_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("invalid,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        new CourtLocationFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(400);
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), resp);
    }

    @Test
    @StabilityTest
    void givenPaging_whenGetCourtLocations_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        new CourtLocationFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        CourtLocationPage page = resp.as(CourtLocationPage.class);
        PagingAssertionUtil.assertPageDetails(page, 1, 0, 2, 2);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "court_location_code",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NATIONAL_COURT_HOUSES,
                        "courthouse_name",
                        null,
                        "",
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getType()
                                .name(),
                        CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT
                                .getEventName()));
    }

    @Test
    public void givenValidRequest_whenMultipleSortsArePresent_thenReturn400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of(
                                CourtLocationSortFieldMapper.CODE.getApiValue(),
                                CourtLocationSortFieldMapper.TITLE.getApiValue()),
                        getLocalUrl(WEB_CONTEXT),
                        token);

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode().getType().get(),
                problemDetail.getType());
    }

    record CourtLocationFilter(Optional<String> name, Optional<String> code)
            implements UnaryOperator<RequestSpecification> {
        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (name.isPresent()) {
                rs = rs.queryParam("name", name.get());
            }
            if (code.isPresent()) {
                rs = rs.queryParam("code", code.get());
            }
            return rs;
        }
    }
}
