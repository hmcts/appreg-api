package uk.gov.hmcts.appregister.report.audit;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class WorkloadReportParameterAudit implements Auditable {
    public static final String TABLE_NAME = "report_parameters";

    private final LocalDate dateFrom;

    private final LocalDate dateTo;

    private final String courtLocationCode;

    private final String otherLocationDescription;

    private final String cjaCode;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> params = new ArrayList<>();
        add(params, "dateFrom", dateFrom);
        add(params, "dateTo", dateTo);
        add(params, "courtLocationCode", courtLocationCode);
        add(params, "otherLocationDescription", otherLocationDescription);
        add(params, "cjaCode", cjaCode);
        return params;
    }

    private void add(List<AuditableData> params, String name, Object value) {
        if (value != null) {
            params.add(new AuditableData(TABLE_NAME, name, value.toString()));
        }
    }

     public static WorkloadReportParameterAudit from(uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto filter) {
        LegacyReportLocation location = filter.getLocation();
        return WorkloadReportParameterAudit.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
                .courtLocationCode(location != null ? filter.getLocation().getCourtLocationCode() : null)
                .otherLocationDescription(location != null ? filter.getLocation().getOtherLocationDescription() : null)
                .cjaCode(location != null ? filter.getLocation().getCjaCode() : null)
                .build();
    }
}
