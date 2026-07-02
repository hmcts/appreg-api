package uk.gov.hmcts.appregister.report.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.appregister.common.log.LogPayloads;
import uk.gov.hmcts.appregister.common.log.PayloadLogDirection;
import uk.gov.hmcts.appregister.common.log.PayloadLogLevel;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;

class ReportControllerPayloadLoggingAnnotationTest {

    @ParameterizedTest
    @MethodSource("annotatedMethods")
    void reportCreationMethods_usePayloadLoggingAnnotation(
            String methodName, Class<?> parameterType, String requestPrefix) throws Exception {
        Method method = ReportController.class.getMethod(methodName, parameterType);
        LogPayloads annotation = method.getAnnotation(LogPayloads.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.direction()).isEqualTo(PayloadLogDirection.BOTH);
        assertThat(annotation.level()).isEqualTo(PayloadLogLevel.INFO);
        assertThat(annotation.requestPrefix()).isEqualTo(requestPrefix);
        assertThat(annotation.responsePrefix()).isEqualTo("Job acknowledgement");
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> annotatedMethods() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "createActivityAuditReport",
                        ActivityAuditFilterDto.class,
                        "Activity Audit Report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createFeesReport", FeesReportFilterDto.class, "Fees report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createWorkloadReport", WorkloadFilterDto.class, "Workload report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createSearchWarrantsReport",
                        SearchWarrantsReportFilterDto.class,
                        "Search warrants report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createDurationReport", DurationFilterDto.class, "Duration report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createListMaintenanceReport",
                        ListMaintenanceFilterDto.class,
                        "List maintenance report payload"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createPrivateProsecutorsIndexReport",
                        PrivateProsecutorsIndexFilterDto.class,
                        "Private Prosecutors Index report payload"));
    }
}
