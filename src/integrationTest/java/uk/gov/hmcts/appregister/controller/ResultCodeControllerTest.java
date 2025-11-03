package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.exception.ResultCodeError;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.AuditAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ResultCodeControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "result-codes";

    // Known seeds (from your resolution_codes seed data)
    private static final String APPC_CODE = "APPC";
    private static final String APPC_TITLE = "Appeal to Crown Court";
    private static final String AUTH_CODE = "AUTH";
    private static final String AUTH_TITLE = "Authorised";
    private static final String CASE_CODE = "CASE";

    private static final LocalDate SEED_START = LocalDate.of(2016, 1, 1);
    private static final LocalDate ACTIVE_DAY = LocalDate.of(2025, 1, 1);

    // Audit event names
    private static final String AUDIT_GET_ONE =
            AuditEventEnum.GET_RESULT_CODE_AUDIT_EVENT.getEventName();
    private static final String AUDIT_GET_PAGE =
            AuditEventEnum.GET_RESULT_CODES_AUDIT_EVENT.getEventName();

    private static final int DEFAULT_PAGE_SIZE = 10;

    // --- /result-codes/{code}?date=YYYY-MM-DD -----------------------------------------------

    @Test
    void givenValidRequest_whenGetResultCodeByCodeAndDate_APPC_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPC_CODE + "?date=" + ACTIVE_DAY), token);

        resp.then().statusCode(200);

        var dto = resp.as(ResultCodeGetDetailDto.class);
        assertThat(dto.getResultCode()).isEqualTo(APPC_CODE);
        assertThat(dto.getTitle()).isEqualTo(APPC_TITLE);
        assertThat(dto.getStartDate()).isEqualTo(SEED_START);
        assertThat(dto.getEndDate().isPresent()).isFalse();

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenValidRequest_whenGetResultCodeByCodeAndDate_AUTH_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + AUTH_CODE + "?date=" + ACTIVE_DAY), token);

        resp.then().statusCode(200);

        var dto = resp.as(ResultCodeGetDetailDto.class);
        assertThat(dto.getResultCode()).isEqualTo(AUTH_CODE);
        assertThat(dto.getTitle()).isEqualTo(AUTH_TITLE);
        assertThat(dto.getStartDate()).isEqualTo(SEED_START);
        assertThat(dto.getEndDate().isPresent()).isFalse();

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenInvalidCode_whenGetResultCodeByCodeAndDate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var invalid = "ZZZ999";
        var resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + invalid + "?date=" + ACTIVE_DAY), token);

        resp.then().statusCode(404);
        ProblemAssertUtil.assertEquals(ResultCodeError.RESULT_CODE_NOT_FOUND.getCode(), resp);

        AuditAssertUtil.assertStart(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertFailCompleted(AUDIT_GET_ONE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenMissingDate_whenGetResultCodeByCodeAndDate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPC_CODE), token);

        resp.then().statusCode(400);
    }

    @Test
    void givenBadDateFormat_whenGetResultCodeByCodeAndDate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPC_CODE + "?date=01-01-2025"), token);

        resp.then().statusCode(400);
    }

    // --- /result-codes (paged, filterable) ---------------------------------------------------

    @Test
    void givenNoFilters_whenGetResultCodes_then200AndContainsExpectedSeeds() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT), token);

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        // Match your default page size
        assertThat(page.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);

        // Don’t assert total count—just presence of known seeds
        assertThat(page.getContent())
                .extracting("resultCode")
                .contains(APPC_CODE, AUTH_CODE, CASE_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenFilterByCodeContains_whenGetResultCodes_then200ContainsAPPC() throws Exception {
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
                        new ResultCodeFilter(Optional.of("ap"), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        assertThat(page.getContent()).extracting("resultCode").contains(APPC_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenFilterByTitleContains_whenGetResultCodes_then200ContainsAUTH() throws Exception {
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
                        new ResultCodeFilter(Optional.empty(), Optional.of("author")),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        assertThat(page.getContent()).extracting("resultCode").contains(AUTH_CODE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenFilterByCodeAndTitle_whenGetResultCodes_then200OnlyCASE() throws Exception {
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
                        new ResultCodeFilter(Optional.of("ca"), Optional.of("case")),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        var codes = page.getContent().stream().map(ResultCodeGetSummaryDto::getResultCode).toList();
        assertThat(codes).contains(CASE_CODE);
        assertThat(codes.stream().filter(CASE_CODE::equals).count()).isEqualTo(1);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenValidSorts_whenGetResultCodes_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        // Sort by API fields validated by ResultCodeSortValidator (title/code)
        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("title,asc", "code,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        new ResultCodeFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        assertThat(page.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Test
    void givenInvalidSort_whenGetResultCodes_then400() throws Exception {
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
                        new ResultCodeFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(400);
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), resp);
    }

    @Test
    void givenPaging_whenGetResultCodes_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1), // size
                        Optional.of(0), // page
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        new ResultCodeFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        resp.then().statusCode(200);

        var page = resp.as(ResultCodePage.class);
        assertThat(page.getPageSize()).isEqualTo(1);

        AuditAssertUtil.assertStart(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(AUDIT_GET_PAGE, logCaptor.getInfoLogs().get(1));
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + APPC_CODE + "?date=" + ACTIVE_DAY))
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

    // --- Filter helper (for query params) ------------------------------------------------------

    record ResultCodeFilter(Optional<String> code, Optional<String> title)
            implements UnaryOperator<io.restassured.specification.RequestSpecification> {
        @Override
        public io.restassured.specification.RequestSpecification apply(
                io.restassured.specification.RequestSpecification rs) {
            if (code.isPresent()) {
                rs = rs.queryParam("code", code.get());
            }
            if (title.isPresent()) {
                rs = rs.queryParam("title", title.get());
            }
            return rs;
        }
    }
}
