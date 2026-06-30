package uk.gov.hmcts.appregister.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.LocalDate;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

class PayloadLoggingAdviceTest {

    private LogCaptor logCaptor;
    private RequestPayloadLoggingAdvice requestAdvice;
    private ResponsePayloadLoggingAdvice responseAdvice;

    @BeforeEach
    void setUp() {
        logCaptor = LogCaptor.forClass(PayloadLogSupport.class);
        logCaptor.clearLogs();

        var support = new PayloadLogSupport(new DefaultJsonPayloadDetector());
        requestAdvice = new RequestPayloadLoggingAdvice(support);
        responseAdvice = new ResponsePayloadLoggingAdvice(support);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void givenAnnotatedJsonRequest_whenBodyRead_thenLogsRequestPayloadAtInfo() throws Exception {
        FeesReportFilterDto body =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.parse("2026-01-01"))
                        .dateTo(LocalDate.parse("2026-01-31"));
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        requestAdvice.afterBodyRead(
                body,
                inputMessage,
                requestParameter("createReport"),
                FeesReportFilterDto.class,
                StringHttpMessageConverter.class);

        assertThat(logCaptor.getInfoLogs().getFirst())
                .contains("Fees report payload:")
                .contains("\"dateFrom\":[2026,1,1]", "\"dateTo\":[2026,1,31]");
    }

    @Test
    void givenAnnotatedJsonResponse_whenBodyWritten_thenLogsResponsePayloadAtDebug()
            throws Exception {
        JobAcknowledgement body =
                new JobAcknowledgement()
                        .id(java.util.UUID.randomUUID())
                        .type(JobType.FEES_REPORT)
                        .status(JobStatus1.RECEIVED);

        responseAdvice.beforeBodyWrite(
                body,
                returnType("debugResponse"),
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                null,
                null);

        assertThat(logCaptor.getDebugLogs().getFirst())
                .contains("Debug response payload:")
                .contains("\"type\":\"FEES_REPORT\"", "\"status\":\"RECEIVED\"");
    }

    @Test
    void givenNonJsonRequest_whenBodyRead_thenDoesNotLogPayload() throws Exception {
        FeesReportFilterDto body =
                new FeesReportFilterDto().dateFrom(LocalDate.parse("2026-01-01"));
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().setContentType(MediaType.TEXT_PLAIN);

        requestAdvice.afterBodyRead(
                body,
                inputMessage,
                requestParameter("createReport"),
                FeesReportFilterDto.class,
                StringHttpMessageConverter.class);

        assertThat(logCaptor.getInfoLogs()).isEmpty();
        assertThat(logCaptor.getDebugLogs()).isEmpty();
    }

    @Test
    void givenNullResponseBody_whenBodyWritten_thenDoesNotLogPayload() throws Exception {
        responseAdvice.beforeBodyWrite(
                null,
                returnType("createReport"),
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                null,
                null);

        assertThat(logCaptor.getInfoLogs()).isEmpty();
        assertThat(logCaptor.getDebugLogs()).isEmpty();
    }

    @Test
    void givenInterfaceMethodParameter_whenImplementationCarriesAnnotation_thenPayloadStillLogs()
            throws Exception {
        FeesReportFilterDto body =
                new FeesReportFilterDto().dateFrom(LocalDate.parse("2026-01-01"));
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        requestAdvice.afterBodyRead(
                body,
                inputMessage,
                interfaceRequestParameter(),
                FeesReportFilterDto.class,
                StringHttpMessageConverter.class);

        assertThat(logCaptor.getInfoLogs().getFirst()).contains("Fees report payload:");
    }

    private static MethodParameter requestParameter(String methodName) throws Exception {
        Method method =
                TestController.class.getDeclaredMethod(methodName, FeesReportFilterDto.class);
        return new MethodParameter(method, 0);
    }

    private static MethodParameter returnType(String methodName) throws Exception {
        Method method =
                TestController.class.getDeclaredMethod(methodName, FeesReportFilterDto.class);
        return new MethodParameter(method, -1);
    }

    private static MethodParameter interfaceRequestParameter() throws Exception {
        Method method =
                TestControllerApi.class.getDeclaredMethod(
                        "createReport", FeesReportFilterDto.class);
        return new MethodParameter(method, 0).withContainingClass(TestController.class);
    }

    interface TestControllerApi {
        JobAcknowledgement createReport(FeesReportFilterDto body);
    }

    static class TestController implements TestControllerApi {
        @PostMapping
        @LogPayloads(requestPrefix = "Fees report payload", responsePrefix = "Job acknowledgement")
        @Override
        public JobAcknowledgement createReport(@RequestBody FeesReportFilterDto body) {
            return new JobAcknowledgement();
        }

        @PostMapping
        @LogPayloads(
                responsePrefix = "Debug response payload",
                direction = PayloadLogDirection.RESPONSE,
                level = PayloadLogLevel.DEBUG)
        JobAcknowledgement debugResponse(@RequestBody FeesReportFilterDto body) {
            return new JobAcknowledgement();
        }
    }
}
