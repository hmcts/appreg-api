package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.audit.model.FeesReportParameterAudit;

class FeesReportParameterAuditTest extends AbstractAsyncTest {
    @Test
    void givenFilter_whenFrom_thenAuditsOnlyReportParameters() {
        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 30))
                        .standardApplicantCode("STD1")
                        .applicantName("Jane Smith")
                        .applicantOrganisation("British Gas")
                        .location(
                                new LegacyReportLocation()
                                        .courtLocationCode("B01IX00")
                                        .otherLocationDescription("Other court")
                                        .cjaCode("01"));

        FeesReportParameterAudit audit = FeesReportParameterAudit.from(filter);

        Assertions.assertNull(audit.getId());
        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30"),
                        new AuditableData("report_parameters", "standardApplicantCode", "STD1"),
                        new AuditableData("report_parameters", "applicantName", "Jane Smith"),
                        new AuditableData(
                                "report_parameters", "applicantOrganisation", "British Gas"),
                        new AuditableData("report_parameters", "courtLocationCode", "B01IX00"),
                        new AuditableData(
                                "report_parameters", "otherLocationDescription", "Other court"),
                        new AuditableData("report_parameters", "cjaCode", "01")),
                audit.extractAuditData(CrudEnum.READ));
    }

    @Test
    void givenNoLocation_whenExtractAuditData_thenSkipsNullValues() {
        FeesReportParameterAudit audit =
                FeesReportParameterAudit.from(
                        new FeesReportFilterDto()
                                .dateFrom(LocalDate.of(2026, 4, 1))
                                .dateTo(LocalDate.of(2026, 4, 30)));

        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30")),
                audit.extractAuditData(CrudEnum.READ));
    }
}
