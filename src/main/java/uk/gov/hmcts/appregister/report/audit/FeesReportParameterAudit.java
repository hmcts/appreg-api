package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.Location;

@Builder
@Getter
public class FeesReportParameterAudit implements Auditable {
    private static final String TABLE_NAME = "report_parameters";

    private final LocalDate dateFrom;

    private final LocalDate dateTo;

    private final String standardApplicantCode;

    private final String applicantName;

    private final String applicantOrganisation;

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
        add(parameters, "standardApplicantCode", standardApplicantCode);
        add(parameters, "applicantName", applicantName);
        add(parameters, "applicantOrganisation", applicantOrganisation);
        add(parameters, "courtLocationCode", courtLocationCode);
        add(parameters, "otherLocationDescription", otherLocationDescription);
        add(parameters, "cjaCode", cjaCode);
        return parameters;
    }

    public static FeesReportParameterAudit from(FeesReportFilterDto filter) {
        Location location = filter.getLocation();
        return FeesReportParameterAudit.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
                .standardApplicantCode(filter.getStandardApplicantCode())
                .applicantName(filter.getApplicantName())
                .applicantOrganisation(filter.getApplicantOrganisation())
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
