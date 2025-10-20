package uk.gov.hmcts.appregister.applicationcode.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;

/**
 * Defines the API sort field names exposed by the Application Code endpoint and the associated
 * mapping onto the backend database.
 *
 * <p>These constants represent the property names that clients/backend can use in sorting
 */
@Getter
public enum ApplicationCodeSortFieldEnum implements SortableOperationEnum {
    TITLE("title", ApplicationCode_.TITLE),
    CODE("code", ApplicationCode_.CODE);

    private final String apiValue;
    private final String entityValue;

    ApplicationCodeSortFieldEnum(String apiValue, String entityValue) {
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
