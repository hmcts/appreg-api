package uk.gov.hmcts.appregister.audit;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea_;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@Getter
public enum AuditEventEnum {
    GET_APPLICATION_CODE_AUDIT_EVENT(
            TableNames.APPLICATION_CODES, ApplicationCode_.CODE, "Get Application Code"),
    GET_APPLICATION_CODES_AUDIT_EVENT(TableNames.APPLICATION_CODES, "N/A", "Get Application Codes"),
    GET_CRIMINAL_JUSTICE_AUDIT_EVENT(
            TableNames.CRIMINAL_JUSTICE_AREA,
            CriminalJusticeArea_.CJA_CODE,
            "Get Court Justice Area");

    private final String tableName;

    private final String columnName;

    private final String eventName;

    AuditEventEnum(String tableName, String columnName, String eventName) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.eventName = eventName;
    }
}
