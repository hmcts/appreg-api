package uk.gov.hmcts.appregister.audit;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@Getter
public enum AuditEnum {
    GET_APPLICATION_CODE_AUDIT_EVENT(
            TableNames.APPLICATION_CODES, ApplicationCode_.CODE, "Get Application Code"),
    GET_APPLICATION_CODES_AUDIT_EVENT(TableNames.APPLICATION_CODES, "N/A", "Get Application Code");

    private final String tableName;

    private final String columnName;

    private final String eventName;

    AuditEnum(String tableName, String columnName, String eventName) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.eventName = eventName;
    }
}
