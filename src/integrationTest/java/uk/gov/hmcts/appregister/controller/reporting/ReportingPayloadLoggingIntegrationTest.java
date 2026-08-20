package uk.gov.hmcts.appregister.controller.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.AppRegExceptionHandler;
import uk.gov.hmcts.appregister.common.log.PayloadLogSupport;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class ReportingPayloadLoggingIntegrationTest extends BaseIntegration {
    private static final String FEES_REPORT_WEB_CONTEXT = "reports/fees/jobs";
    private static final String SENSITIVE_MARKER = "SensitiveMarker123";

    private LogCaptor payloadLogCaptor;
    private LogCaptor exceptionHandlerLogCaptor;

    @BeforeEach
    void setUpPayloadLogCaptor() {
        payloadLogCaptor = LogCaptor.forClass(PayloadLogSupport.class);
        exceptionHandlerLogCaptor = LogCaptor.forClass(AppRegExceptionHandler.class);
        payloadLogCaptor.clearLogs();
        exceptionHandlerLogCaptor.clearLogs();
    }

    @AfterEach
    void clearPayloadLogCaptor() {
        payloadLogCaptor.clearLogs();
        exceptionHandlerLogCaptor.clearLogs();
    }

    @Test
    void givenFeesReportRequest_whenEndpointCalled_thenPayloadLogsAreNotWritten() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(7))
                        .dateTo(LocalDate.now(java.time.ZoneOffset.UTC));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        JobAcknowledgement acknowledgement = createResponse.as(JobAcknowledgement.class);

        assertThat(acknowledgement.getType()).isEqualTo(JobType.FEES_REPORT);
        assertThat(acknowledgement.getStatus()).isEqualTo(JobStatus.RECEIVED);
        assertThat(payloadLogCaptor.getInfoLogs()).isEmpty();
        assertThat(payloadLogCaptor.getDebugLogs()).isEmpty();

        JobAcknowledgement terminalStatus =
                AwaitilityUtil.waitForJobToReachTerminalStatus(
                        restAssuredClient,
                        getLocalUrl("jobs/" + acknowledgement.getId()),
                        tokenGenerator.fetchTokenForRole(),
                        Duration.ofSeconds(30));

        assertThat(terminalStatus.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(payloadLogCaptor.getInfoLogs()).isEmpty();
        assertThat(payloadLogCaptor.getDebugLogs()).isEmpty();
    }

    @Test
    void givenReportRequestWithUnknownField_whenRejected_thenRawPayloadIsNotLogged()
            throws Exception {
        String request =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "applicantName": "%s",
                  "unexpectedField": "unexpectedValue"
                }
                """
                        .formatted(SENSITIVE_MARKER);

        Response response = executeFeesReportRequest(request);

        response.then().statusCode(400);
        assertThat(response.as(ProblemDetail.class).getDetail())
                .isEqualTo("Unsupported request field: unexpectedField");
        assertThat(exceptionHandlerLogCaptor.getWarnLogs())
                .anyMatch(log -> log.contains("[400]: Unsupported request field: unexpectedField"));
        assertNoPayloadMarkerWasLogged();
    }

    @Test
    void givenReportRequestWithInvalidFieldType_whenRejected_thenRawPayloadIsNotLogged()
            throws Exception {
        String request =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "applicantName": ["%s"]
                }
                """
                        .formatted(SENSITIVE_MARKER);

        Response response = executeFeesReportRequest(request);

        response.then().statusCode(400);
        assertThat(response.as(ProblemDetail.class).getDetail())
                .isEqualTo("Type conversion problem. Something in the payload is not correct");
        assertThat(exceptionHandlerLogCaptor.getWarnLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "[400]: Type conversion problem. Something in the payload is not correct"));
        assertNoPayloadMarkerWasLogged();
    }

    private Response executeFeesReportRequest(String request) throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        return restAssuredClient.executePostRequest(
                getLocalUrl(FEES_REPORT_WEB_CONTEXT), tokenGenerator.fetchTokenForRole(), request);
    }

    private void assertNoPayloadMarkerWasLogged() {
        assertThat(exceptionHandlerLogCaptor.getLogs())
                .noneMatch(log -> log.contains(SENSITIVE_MARKER));
        assertThat(payloadLogCaptor.getLogs()).isEmpty();
    }
}
