package uk.gov.hmcts.appregister.applicationentry.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntrySortFieldEnum implements SortableOperationEnum {
    // ensure we always add id to guarantee a consistent sort order in searches
    CODE("courtCode", "id", "courtCode"),
    LOCATION("otherLocationDescription", "id", "otherLocationDescription"),
    LEGISLATION("legislation", "id", "legislation"),
    CJA_CODE("cjaCode", "id", "cjaCode"),
    ACCOUNT_REFERENCE("accountReference", "id", "accountReference"),
    STATUS("status", "id", "status"),
    DATE("hearingDate", "id", "date");
    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    ApplicationEntrySortFieldEnum(String apiValue, String tieBreaker, String... entityValue) {
        this.apiValue = apiValue;
        this.entityValue = entityValue;
        this.tieBreaker = tieBreaker;
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
