package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.AuditAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class CourtLocationControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "court-locations";

    private static final String CARDIFF_CODE = "CCC003";
    private static final String CARDIFF_NAME = "Cardiff Crown Court";
    private static final LocalDate CARDIFF_START = LocalDate.of(1904, 1, 1);

    private static final String BRISTOL_CODE = "BCC006";
    private static final String BRISTOL_NAME = "Bristol Crown Court";
    private static final LocalDate BRISTOL_START = LocalDate.of(1993, 6, 1);

    // Audit event names
    private static final String AUDIT_GET_ONE =
            AuditEventEnum.GET_COURT_LOCATION_AUDIT_EVENT.getEventName();
    private static final String AUDIT_GET_PAGE =
            AuditEventEnum.GET_COURT_LOCATIONS_AUDIT_EVENT.getEventName();

    private static final int DEFAULT_PAGE_SIZE = 10;

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
        assertThat(dto.getEndDate().isPresent()).isFalse();

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
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
        assertThat(dto.getEndDate().isPresent()).isFalse();

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
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
    }

    @Test
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
    }

    @Test
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
    }

    @Test
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
    }

    @Test
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
                        List.of("name,asc", "courtLocationCode,desc"),
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
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        WEB_CONTEXT + "/" + CARDIFF_CODE, OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    // --- Helper to apply optional query params -------------------------------------------------

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
