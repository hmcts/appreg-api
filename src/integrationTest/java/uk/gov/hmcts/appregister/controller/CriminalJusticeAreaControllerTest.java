package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.courtlocation.audit.CourtLocationAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.api.CriminalJusticeSortFieldEnum;
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class CriminalJusticeAreaControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "criminal-justice-areas";
    // expectations based on the flyway test data
    private static final String EXPECTED_CODE = "CD";
    private static final String EXPECTED_DESCRIPTION = "CJA_CD_DESCRIPTION";

    private static final String EXPECTED_CODE1 = "CE";
    private static final String EXPECTED_DESCRIPTION1 = "CJA_CE_DESCRIPTION";

    private static final String EXPECTED_CODE2 = "CJ";
    private static final String EXPECTED_DESCRIPTION2 = "CJA_DESCRIPTION";

    // audit expectations
    private static final String EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION =
            CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getEventName();
    private static final String EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION =
            CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName();

    private static final Integer DEFAULT_PAGE_SIZE = 10;

    // The total criminal justice area inserted by flyway scripts. See V6__InitialTestData.sql
    private static final int TOTAL_CJA_COUNT = 4;

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeAreaWithValidCode_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + EXPECTED_CODE),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaGetDto responseContent =
                responseSpec.as(CriminalJusticeAreaGetDto.class);

        // assert the core payload
        CriminalJusticeAreaGetDto criminalJusticeAreaDto =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        // our generated dtos contain an implemented logical equivalence
        Assertions.assertEquals(criminalJusticeAreaDto, responseContent);

        // assert the audit log message
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                EXPECTED_CODE,
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getEventName()));
    }

    @Test
    public void givenValidRequest_whenGetCriminalJusticeAreaWithInvalidCode_thenReturn404()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String invalidCode = "IN";

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + invalidCode),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                CriminalJusticeAreaError.CJA_NOT_FOUND.getCode(), responseSpec);

        // assert the audit log message
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertFailCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeArea_thenReturn200() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(
                responseContent, DEFAULT_PAGE_SIZE, 0, 1, TOTAL_CJA_COUNT);

        // assert the expected default order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        CriminalJusticeAreaGetDto expectedCriminalJusticeArea2 =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE1, EXPECTED_DESCRIPTION1);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);
        Assertions.assertEquals(actualResponse.getContent().get(1), expectedCriminalJusticeArea2);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeAreaWithPaging_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        int pageSize = 2;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(responseContent, 2, 0, 2, TOTAL_CJA_COUNT);

        // assert the expected default order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        CriminalJusticeAreaGetDto expectedCriminalJusticeArea2 =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE1, EXPECTED_DESCRIPTION1);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);
        Assertions.assertEquals(actualResponse.getContent().get(1), expectedCriminalJusticeArea2);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeAreaSort_thenReturn200() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("code,desc", "description,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(Optional.empty(), Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(
                responseContent, DEFAULT_PAGE_SIZE, 0, 1, TOTAL_CJA_COUNT);

        // assert the expected order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE2, EXPECTED_DESCRIPTION2);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);
        Assertions.assertEquals(actualResponse.getContent().get(1), expectedCriminalJusticeArea);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Test
    public void givenValidRequest_whenGetCriminalJusticeAreaInvalidSort_thenReturn400()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("invalidsort"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(
                                Optional.of(EXPECTED_CODE.toLowerCase()), Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), responseSpec);
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeAreaFilterByCode_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        int pageSize = 2;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(
                                Optional.of(EXPECTED_CODE.toLowerCase()), Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(responseContent, 2, 0, 1, 1);

        // assert the expected default order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                EXPECTED_CODE.toLowerCase(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetCriminalJusticeAreaFilterByDescription_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        int pageSize = 2;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(
                                Optional.empty(), Optional.of(EXPECTED_DESCRIPTION.toLowerCase())),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(responseContent, 2, 0, 1, 1);

        // assert the expected default order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                EXPECTED_DESCRIPTION.toLowerCase(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    public void
            givenValidRequest_whenGetCriminalJusticeAreaFilterByCodeAndDescription_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        int pageSize = 2;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(
                                Optional.of(EXPECTED_CODE.toLowerCase()),
                                Optional.of(EXPECTED_DESCRIPTION.toLowerCase())),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaPage responseContent = responseSpec.as(CriminalJusticeAreaPage.class);
        PagingAssertionUtil.assertPageDetails(responseContent, 2, 0, 1, 1);

        // assert the expected default order and content
        CriminalJusticeAreaGetDto expectedCriminalJusticeArea =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        CriminalJusticeAreaPage actualResponse = responseSpec.as(CriminalJusticeAreaPage.class);
        Assertions.assertEquals(actualResponse.getContent().get(0), expectedCriminalJusticeArea);

        // assert the log
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                EXPECTED_CODE.toLowerCase(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                EXPECTED_DESCRIPTION.toLowerCase(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @StabilityTest
    public void givenCriminalJusticeSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws Exception {
        for (CriminalJusticeSortFieldEnum criminalJusticeSortFieldEnum :
                CriminalJusticeSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(criminalJusticeSortFieldEnum.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            CriminalJusticeAreaPage page = responseSpec.as(CriminalJusticeAreaPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            Assertions.assertEquals(
                    criminalJusticeSortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());

            differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                    TableNames.CRIMINAL_JUSTICE_AREA,
                    "cja_code",
                    null,
                    "",
                    CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                    CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                    TableNames.CRIMINAL_JUSTICE_AREA,
                    "cja_description",
                    null,
                    "",
                    CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                    CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
        }

        Assertions.assertTrue(CriminalJusticeSortFieldEnum.values().length > 0);
    }

    private CriminalJusticeAreaGetDto generateDefaultCriminalJusticeDtoAssertionPayload(
            String code, String description) {
        return new CriminalJusticeAreaGetDto(code, description);
    }

    @StabilityTest
    public void givenASuccessfulFilterPartialDescription_whenSearch_thenSuccessResponse()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new CriminalJusticeAreaFilter(Optional.empty(), Optional.of("_c")),
                        new OpenApiPageMetaData());

        CriminalJusticeAreaPage page = responseSpec.as(CriminalJusticeAreaPage.class);

        // make sure the order response marries with the request data
        responseSpec.then().statusCode(200);
        Assertions.assertEquals(2, page.getContent().size());
        Assertions.assertEquals("CD", page.getContent().get(0).getCode());
        Assertions.assertEquals("CE", page.getContent().get(1).getCode());

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_code",
                null,
                "",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.CRIMINAL_JUSTICE_AREA,
                "cja_description",
                null,
                "_c",
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getType().name(),
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName()));
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        WEB_CONTEXT + "/" + EXPECTED_CODE, OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrlWithDate(WEB_CONTEXT, OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    /**
     * A request specification that knows what query filters can be applied to get criminal justice
     * areas.
     */
    @RequiredArgsConstructor
    static class CriminalJusticeAreaFilter implements UnaryOperator<RequestSpecification> {
        private final Optional<String> code;
        private final Optional<String> description;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (code.isPresent()) {
                rs = rs.queryParam("code", code.get());
            }

            if (description.isPresent()) {
                rs = rs.queryParam("description", description.get());
            }

            return rs;
        }
    }
}
