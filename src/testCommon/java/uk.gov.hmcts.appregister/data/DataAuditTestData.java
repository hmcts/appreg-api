package uk.gov.hmcts.appregister.data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.util.StringUtil;

public class DataAuditTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                DataAudit, DataAudit.DataAuditBuilder> {

    @Override
    public DataAudit.DataAuditBuilder someMinimal() {
        DataAudit.DataAuditBuilder dataAudit = DataAudit.builder();
        UUID uniqueId = UUID.randomUUID();
        dataAudit
                .schemaName(StringUtil.stripToMax(uniqueId.toString(), 30))
                .tableName(StringUtil.stripToMax("table" + uniqueId.toString(), 30))
                .columnName(StringUtil.stripToMax("column" + uniqueId.toString(), 30))
                .changedDate(OffsetDateTime.now(ZoneId.of("UTC")))
                .updateType("1");
        return dataAudit;
    }
}
