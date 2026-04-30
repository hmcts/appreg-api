package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;

class ActivityAuditReportParameterAuditTest {
    @Test
    void givenFilter_whenFrom_thenAuditsOnlyReportParameters() {
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 30))
                        .username("caseworker")
                        .activityTypes(
                                List.of(
                                        ActivityType.ADD_APPLICATION,
                                        ActivityType.UPDATE_APPLICATION));

        ActivityAuditReportParameterAudit audit = ActivityAuditReportParameterAudit.from(filter);

        Assertions.assertNull(audit.getId());
        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30"),
                        new AuditableData("report_parameters", "username", "caseworker"),
                        new AuditableData(
                                "report_parameters",
                                "activityTypes",
                                "ADD_APPLICATION,UPDATE_APPLICATION")),
                audit.extractAuditData(CrudEnum.READ));
    }

    @Test
    void givenNullOptionalParameters_whenExtractAuditData_thenSkipsNullValues() {
        ActivityAuditReportParameterAudit audit =
                ActivityAuditReportParameterAudit.builder()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 30))
                        .build();

        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30")),
                audit.extractAuditData(CrudEnum.READ));
    }
}
