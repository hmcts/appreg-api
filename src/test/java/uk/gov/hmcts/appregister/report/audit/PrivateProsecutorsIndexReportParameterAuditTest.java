package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;

class PrivateProsecutorsIndexReportParameterAuditTest {
    @Test
    void givenFilter_whenFrom_thenAuditsOnlyReportParameters() {
        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 30))
                        .applicantSurname("Smith")
                        .applicantFirstName("John")
                        .applicantOrganisationName("Acme")
                        .standardApplicantName("CPS")
                        .respondentSurname("Bloggs")
                        .respondentFirstName("Jane")
                        .respondentOrganisationName("Widgets")
                        .location(
                                new LegacyReportLocation()
                                        .otherLocationDescription("Other court")
                                        .cjaCode("01"));

        PrivateProsecutorsIndexReportParameterAudit audit =
                PrivateProsecutorsIndexReportParameterAudit.from(filter);

        Assertions.assertNull(audit.getId());
        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30"),
                        new AuditableData("report_parameters", "applicantSurname", "Smith"),
                        new AuditableData("report_parameters", "applicantFirstName", "John"),
                        new AuditableData("report_parameters", "applicantOrganisationName", "Acme"),
                        new AuditableData("report_parameters", "standardApplicantName", "CPS"),
                        new AuditableData("report_parameters", "respondentSurname", "Bloggs"),
                        new AuditableData("report_parameters", "respondentFirstName", "Jane"),
                        new AuditableData(
                                "report_parameters", "respondentOrganisationName", "Widgets"),
                        new AuditableData(
                                "report_parameters", "otherLocationDescription", "Other court"),
                        new AuditableData("report_parameters", "cjaCode", "01")),
                audit.extractAuditData(CrudEnum.READ));
    }

    @Test
    void givenNoLocationOrOptionalFilters_whenExtractAuditData_thenSkipsNullValues() {
        PrivateProsecutorsIndexReportParameterAudit audit =
                PrivateProsecutorsIndexReportParameterAudit.from(
                        new PrivateProsecutorsIndexFilterDto()
                                .dateFrom(LocalDate.of(2026, 4, 1))
                                .dateTo(LocalDate.of(2026, 4, 30)));

        Assertions.assertEquals(
                List.of(
                        new AuditableData("report_parameters", "dateFrom", "2026-04-01"),
                        new AuditableData("report_parameters", "dateTo", "2026-04-30")),
                audit.extractAuditData(CrudEnum.READ));
    }
}
