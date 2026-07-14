package uk.gov.hmcts.appregister.csds.ingress.audit;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@Builder
@Getter
public class CsdsIngestAudit implements Auditable {
    private static final String TABLE_NAME = "csds_ingest_runs";

    private final String requestingUser;
    private final String processorName;
    private final String fileName;
    private final Long fileSize;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> data = new ArrayList<>();
        add(data, "requestingUser", requestingUser);
        add(data, "processorName", processorName);
        add(data, "fileName", fileName);
        add(data, "fileSize", fileSize);
        return data;
    }

    private void add(List<AuditableData> data, String name, Object value) {
        if (value != null) {
            data.add(new AuditableData(TABLE_NAME, name, value.toString()));
        }
    }
}
