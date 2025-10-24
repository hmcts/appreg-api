package uk.gov.hmcts.appregister.resultcode.api;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode_;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the API sort field names exposed by the Result Code endpoint and the associated
 * mapping onto the backend database.
 *
 * <p>These constants represent the property names that clients/backend can use in sorting
 */
@Getter
public enum ResultCodeSortFieldEnum implements SortableOperationEnum {
    TITLE("title", ResolutionCode_.TITLE),
    CODE("resultCode", ResolutionCode_.RESULT_CODE);

    private final String apiValue;
    private final String entityValue;

    ResultCodeSortFieldEnum(String apiValue, String entityValue) {
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
