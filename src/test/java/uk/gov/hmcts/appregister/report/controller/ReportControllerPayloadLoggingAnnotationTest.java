package uk.gov.hmcts.appregister.report.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.appregister.common.log.LogPayloads;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;

class ReportControllerPayloadLoggingAnnotationTest {

    @ParameterizedTest
    @MethodSource("reportCreationMethods")
    void reportCreationMethods_doNotUsePayloadLoggingAnnotation(
            String methodName, Class<?> parameterType) throws Exception {
        Method method = ReportController.class.getMethod(methodName, parameterType);
        LogPayloads annotation = method.getAnnotation(LogPayloads.class);

        assertThat(annotation).isNull();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> reportCreationMethods() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "createActivityAuditReport", ActivityAuditFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createFeesReport", FeesReportFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createWorkloadReport", WorkloadFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createSearchWarrantsReport", SearchWarrantsReportFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createDurationReport", DurationFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createListMaintenanceReport", ListMaintenanceFilterDto.class),
                org.junit.jupiter.params.provider.Arguments.of(
                        "createPrivateProsecutorsIndexReport",
                        PrivateProsecutorsIndexFilterDto.class));
    }
}
