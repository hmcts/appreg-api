package uk.gov.hmcts.appregister.report.audit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;

@Builder
@Getter
public class ActivityAuditReportParameterAudit implements Auditable {
    private static final String TABLE_NAME = "report_parameters";

    private final LocalDate dateFrom;

    private final LocalDate dateTo;

    private final String username;

    private final String activityTypes;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> parameters = new ArrayList<>();
        add(parameters, "dateFrom", dateFrom);
        add(parameters, "dateTo", dateTo);
        add(parameters, "username", username);
        add(parameters, "activityTypes", activityTypes);
        return parameters;
    }

    public static ActivityAuditReportParameterAudit from(ActivityAuditFilterDto filter) {
        return ActivityAuditReportParameterAudit.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
                .username(filter.getUsername())
                .activityTypes(
                        filter.getActivityTypes().stream()
                                .map(Enum::toString)
                                .collect(Collectors.joining(",")))
                .build();
    }

    private void add(List<AuditableData> parameters, String name, Object value) {
        if (value != null) {
            parameters.add(new AuditableData(TABLE_NAME, name, value.toString()));
        }
    }
}
