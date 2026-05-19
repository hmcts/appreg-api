package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;

class DurationReportParameterAuditTest {
    @Test
    void givenFilter_whenFrom_thenAuditsOnlyReportParameters() {
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 30))
                        .location(
                                new LegacyReportLocation()
                                        .otherLocationDescription("Other court")
                                        .cjaCode("01"));

        DurationReportParameterAudit audit = DurationReportParameterAudit.from(filter);

        Assertions.assertNull(audit.getId());
        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30"),
                        new AuditableData(
                                "report_parameters", "otherLocationDescription", "Other court"),
                        new AuditableData("report_parameters", "cjaCode", "01")),
                audit.extractAuditData(CrudEnum.READ));
    }

    @Test
    void givenNoLocation_whenExtractAuditData_thenSkipsNullValues() {
        DurationReportParameterAudit audit =
                DurationReportParameterAudit.from(
                        new DurationFilterDto()
                                .dateFrom(LocalDate.of(2026, 4, 1))
                                .dateTo(LocalDate.of(2026, 4, 30)));

        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30")),
                audit.extractAuditData(CrudEnum.READ));
    }
}
