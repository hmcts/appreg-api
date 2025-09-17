package uk.gov.hmcts.appregister.apllicationcode.controller;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import joptsimple.util.RegexMatcher;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.applicationcode.service.ApplicationCodeServiceImpl;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ApplicationCodeControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-codes";

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @MockitoBean private Clock clock; // replaces Clock bean in Spring context

    /** The total app codes inserted by flyway scripts. See V6__InitialTestData.sql */
    private static final int TOTAL_APP_CODES_COUNT = 42;

    @BeforeEach
    public void before() {
        // a date that is without range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto[] codeDto = responseSpec.as(ApplicationCodeDto[].class);
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT, codeDto.length);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(codeDto[1], applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER.getRole())).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto[] codeDto = responseSpec.as(ApplicationCodeDto[].class);
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT, codeDto.length);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(codeDto[1], applicationCodeDto);

        RegexMatcher.regex(".*");

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithoutOffsiteFee_thenReturn200()
            throws Exception {
        // a date that is within range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.parse("2014-07-25T10:15:30Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto[] codeDto = responseSpec.as(ApplicationCodeDto[].class);
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT, codeDto.length);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(50.0),
                        Optional.empty(),
                        Optional.empty());

        assertApplicationCode(codeDto[1], applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithOffsiteFeeButNoMain_thenReturn200()
            throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse("2020-07-25T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto[] codeDto = responseSpec.as(ApplicationCodeDto[].class);
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT, codeDto.length);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(70.0));

        assertApplicationCode(codeDto[1], applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Codes\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("2016-01-01T00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert the first auth code record
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(codeDto, applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER.getRole())).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("2016-01-01T00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert the first auth code record
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(codeDto, applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeWithoutOffsite_thenReturn200()
            throws Exception {
        // a date that is within range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.parse("2014-07-25T10:15:30Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("2016-01-01T00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(50.0),
                        Optional.empty(),
                        Optional.empty());

        assertApplicationCode(codeDto, applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithOffsiteFeeButNoMain_thenReturn200()
                    throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse("2020-07-25T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("2016-01-01T00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(70.0));

        assertApplicationCode(codeDto, applicationCodeDto);

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeNotValid_thenReturn404()
            throws Exception {
        String id = "doesntexist";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(WEB_CONTEXT + "/" + id, OffsetDateTime.now()),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN.getRole()))
                                .build()
                                .fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemDetail codeDto = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getType().get(), codeDto.getType());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getDetail());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getTitle());
        Assertions.assertEquals("/" + WEB_CONTEXT + "/" + id, codeDto.getInstance().toString());

        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion fail audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=-1\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForDateNotValid_thenReturn404()
            throws Exception {
        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("1916-01-01T00:00Z")),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN.getRole()))
                                .build()
                                .fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemDetail codeDto = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getType().get(), codeDto.getType());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getDetail());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getTitle());
        Assertions.assertEquals("/" + WEB_CONTEXT + "/" + id, codeDto.getInstance().toString());

        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion fail audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=-1\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesReturnsMultipleRecords_thenReturn200WithFirstRecord()
                    throws Exception {

        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse("2020-07-25T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        String id = "AD99002";
        LogCaptor appCodeServiceLogCaptor = LogCaptor.forClass(ApplicationCodeServiceImpl.class);

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id,
                                OffsetDateTime.parse("2016-01-01T00:00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(70.0));

        assertApplicationCode(codeDto, applicationCodeDto);

        // assert that we see a warning in the logs
        Assertions.assertTrue(
                appCodeServiceLogCaptor
                        .getWarnLogs()
                        .get(0)
                        .startsWith("Too many records found for code"));

        // assert the audit log message
        Assertions.assertTrue(
                Pattern.matches(
                        "Start audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=1\n"
                                + "-p_messagecontent=NULL",
                        logCaptor.getInfoLogs().get(0)));

        Assertions.assertTrue(
                Pattern.matches(
                        "Completion audit \n"
                                + "-p_requestaction=Get Application Code\n"
                                + "-p_messageuuid=.*\n"
                                + "-p_messagestatus=10\n"
                                + "-p_messagecontent=.*",
                        logCaptor.getInfoLogs().get(1)));
    }

    private ApplicationCodeDto generateDefaultApplicationCodeDtoAssertionPayload(
            Optional<String> mainFeeDesc,
            Optional<Double> mainFeeAmt,
            Optional<String> offsiteFeeDesc,
            Optional<Double> offsiteFeeAmt) {
        return new ApplicationCodeDto(
                1L,
                "AD99002",
                "Copy documents (electronic)",
                "Request for copy documents on computer" + " disc or in electronic form",
                "",
                true,
                false,
                "address1@cgi.com",
                "address2@cgi.com",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                false,
                "CO1.1",
                mainFeeDesc.orElse(null),
                mainFeeAmt.orElse(null),
                offsiteFeeDesc.orElse(null),
                offsiteFeeAmt.orElse(null),
                OffsetDateTime.now(),
                "Jane Doe",
                "Code wording");
    }

    private void assertApplicationCode(ApplicationCodeDto actual, ApplicationCodeDto expected) {
        Assertions.assertEquals(expected.applicationCode(), actual.applicationCode());
        Assertions.assertEquals(expected.title(), actual.title());
        Assertions.assertEquals(expected.wording(), actual.wording());
        Assertions.assertEquals(expected.feeDue(), actual.feeDue());
        Assertions.assertEquals(expected.requiresRespondent(), actual.requiresRespondent());

        if (expected.startDate() == null) {
            Assertions.assertNull(actual.startDate());
        } else {
            Assertions.assertNotNull(expected.startDate());
        }

        if (expected.endDate() == null) {
            Assertions.assertNull(actual.endDate());
        } else {
            Assertions.assertNotNull(expected.endDate());
        }

        Assertions.assertEquals(expected.bulkRespondentAllowed(), actual.bulkRespondentAllowed());
        Assertions.assertEquals(expected.feeReference(), actual.feeReference());
        Assertions.assertEquals(expected.mainFeeAmount(), actual.mainFeeAmount());
        Assertions.assertEquals(expected.offsetFeeDescription(), actual.offsetFeeDescription());
        Assertions.assertEquals(expected.offsetFeeAmount(), actual.offsetFeeAmount());

        if (expected.lodgementDate() == null) {
            Assertions.assertNull(actual.lodgementDate());
        } else {
            Assertions.assertNotNull(expected.lodgementDate());
        }
        Assertions.assertEquals(expected.applicantName(), actual.applicantName());
        Assertions.assertEquals(expected.destinationEmail1(), actual.destinationEmail1());
        Assertions.assertEquals(expected.destinationEmail2(), actual.destinationEmail2());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl("application-codes"))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrlWithDate("application-codes/2", OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
