package uk.gov.hmcts.appregister.applicationentry.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntrySortFieldEnum implements SortableOperationEnum {
    // ensure we always add id to guarantee a consistent sort order in searches
    CODE("courtCode", "courtCode", "id"),
    LOCATION("otherLocationDescription", "otherLocationDescription", "id"),
    LEGISLATION("legislation", "legislation", "id"),
    CJA_CODE("cjaCode", "cjaCode", "id"),
    ACCOUNT_REFERENCE("accountReference", "accountReference", "id"),
    STATUS("status", "status", "id"),
    DATE("hearingDate", "date", "id");
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
