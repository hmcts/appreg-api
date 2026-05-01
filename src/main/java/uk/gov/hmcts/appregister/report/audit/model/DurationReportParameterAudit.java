package uk.gov.hmcts.appregister.report.audit.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.common.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;

@Builder
@Getter
public class DurationReportParameterAudit implements Auditable {
    private static final String TABLE_NAME = "report_parameters";

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
        List<AuditableData> parameters = new ArrayList<>();
        add(parameters, "dateFrom", dateFrom);
        add(parameters, "dateTo", dateTo);
        add(parameters, "courtLocationCode", courtLocationCode);
        add(parameters, "otherLocationDescription", otherLocationDescription);
        add(parameters, "cjaCode", cjaCode);
        return parameters;
    }

    public static DurationReportParameterAudit from(DurationFilterDto filter) {
        LegacyReportLocation location = filter.getLocation();
        return DurationReportParameterAudit.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
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
