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

    private LogCaptor payloadLogCaptor;

    @BeforeEach
    void setUpPayloadLogCaptor() {
        payloadLogCaptor = LogCaptor.forClass(PayloadLogSupport.class);
        payloadLogCaptor.clearLogs();
    }

    @AfterEach
    void clearPayloadLogCaptor() {
        payloadLogCaptor.clearLogs();
    }

    @Test
    void givenFeesReportRequest_whenEndpointCalled_thenPayloadLogsAreWritten() throws Exception {
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
        assertThat(payloadLogCaptor.getInfoLogs())
                .anySatisfy(
                        log ->
                                assertThat(log)
                                        .contains("Fees report payload:")
                                        .contains("\"dateFrom\"")
                                        .contains("\"dateTo\""))
                .anySatisfy(
                        log ->
                                assertThat(log)
                                        .contains("Job acknowledgement:")
                                        .contains("\"type\":\"FEES_REPORT\"")
                                        .contains("\"status\":\"RECEIVED\""));

        JobAcknowledgement terminalStatus =
                AwaitilityUtil.waitForJobToReachTerminalStatus(
                        restAssuredClient,
                        getLocalUrl("jobs/" + acknowledgement.getId()),
                        tokenGenerator.fetchTokenForRole(),
                        Duration.ofSeconds(30));

        assertThat(terminalStatus.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }
}
