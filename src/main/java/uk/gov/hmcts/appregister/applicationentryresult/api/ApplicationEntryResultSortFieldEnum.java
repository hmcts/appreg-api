package uk.gov.hmcts.appregister.applicationentryresult.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntryResultSortFieldEnum implements SortableOperationEnum {
    // ensure we always add id to guarantee a consistent sort order in searches
    CODE("resolutionCode", "id", "resolutionCode");
    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    ApplicationEntryResultSortFieldEnum(String apiValue, String tieBreaker, String... entityValue) {
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
