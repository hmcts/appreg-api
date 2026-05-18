package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;

@Builder
@Getter
public class PrivateProsecutorsIndexReportParameterAudit implements Auditable {
    private static final String TABLE_NAME = "report_parameters";

    private final LocalDate dateFrom;

    private final LocalDate dateTo;

    private final String applicantSurname;

    private final String applicantFirstName;

    private final String applicantOrganisationName;

    private final String standardApplicantName;

    private final String respondentSurname;

    private final String respondentFirstName;

    private final String respondentOrganisationName;

    private final String courtLocationCode;

    private final String otherLocationDescription;

    private final String cjaCode;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> parameters = new ArrayList<>();
        add(parameters, "dateFrom", dateFrom);
        add(parameters, "dateTo", dateTo);
        add(parameters, "applicantSurname", applicantSurname);
        add(parameters, "applicantFirstName", applicantFirstName);
        add(parameters, "applicantOrganisationName", applicantOrganisationName);
        add(parameters, "standardApplicantName", standardApplicantName);
        add(parameters, "respondentSurname", respondentSurname);
        add(parameters, "respondentFirstName", respondentFirstName);
        add(parameters, "respondentOrganisationName", respondentOrganisationName);
        add(parameters, "courtLocationCode", courtLocationCode);
        add(parameters, "otherLocationDescription", otherLocationDescription);
        add(parameters, "cjaCode", cjaCode);
        return parameters;
    }

    public static PrivateProsecutorsIndexReportParameterAudit from(
            PrivateProsecutorsIndexFilterDto filter) {
        LegacyReportLocation location = filter.getLocation();
        return PrivateProsecutorsIndexReportParameterAudit.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
                .applicantSurname(filter.getApplicantSurname())
                .applicantFirstName(filter.getApplicantFirstName())
                .applicantOrganisationName(filter.getApplicantOrganisationName())
                .standardApplicantName(filter.getStandardApplicantName())
                .respondentSurname(filter.getRespondentSurname())
                .respondentFirstName(filter.getRespondentFirstName())
                .respondentOrganisationName(filter.getRespondentOrganisationName())
                .courtLocationCode(location != null ? location.getCourtLocationCode() : null)
                .otherLocationDescription(
                        location != null ? location.getOtherLocationDescription() : null)
                .cjaCode(location != null ? location.getCjaCode() : null)
                .build();
    }

    private void add(List<AuditableData> parameters, String name, Object value) {
        if (value != null) {
            parameters.add(new AuditableData(TABLE_NAME, name, value.toString()));
        }
    }
}
