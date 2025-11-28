package uk.gov.hmcts.appregister.applicationentry.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntrySortFieldEnum implements SortableOperationEnum {
    CODE("courtCode", "courtCode"),
    LOCATION("otherLocationDescription", "otherLocationDescription"),
    LEGISLATION("legislation", "legislation"),
    CJA_CODE("cjaCode", "cjaCode"),
    ACCOUNT_REFERENCE("accountReference", "accountReference"),
    STATUS("status", "status"),
    DATE("hearingDate", "date");
    private final String apiValue;
    private final String[] entityValue;

    ApplicationEntrySortFieldEnum(String apiValue, String... entityValue) {
        this.apiValue = apiValue;
        this.entityValue = entityValue;
    }

    private static final Map<String, SortableOperationEnum> MAPPINGS = new HashMap<>();

    static {
        for (SortableOperationEnum status : values()) {
            MAPPINGS.put(status.getApiValue(), status);
        }
    }

    public static SortableOperationEnum getEntityValue(String apiValue) {
        return MAPPINGS.get(apiValue);
    }
}
