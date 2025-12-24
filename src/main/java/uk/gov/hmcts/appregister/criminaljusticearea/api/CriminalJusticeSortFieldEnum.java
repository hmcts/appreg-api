package uk.gov.hmcts.appregister.criminaljusticearea.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea_;

@Getter
public enum CriminalJusticeSortFieldEnum implements SortableOperationEnum {
    CODE("code", CriminalJusticeArea_.CODE),
    DESCRIPTION("description", CriminalJusticeArea_.DESCRIPTION);

    private final String apiValue;
    private final String[] entityValue;

    CriminalJusticeSortFieldEnum(String apiValue, String... entityValue) {
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
