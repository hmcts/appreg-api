package uk.gov.hmcts.appregister.applicationlist.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry_;

/**
 * Defines the API field names exposed by the Application List endpoints.
 *
 * <p>These constants represent the property names that clients can use in filters, sorting, or
 * query parameters.
 */
@Getter
public enum ApplicationListEntriesSummarySortFieldEnum implements SortableOperationEnum {
    SEQUENCE_NUMBER(ApplicationListEntry_.SEQUENCE_NUMBER, ApplicationListEntry_.SEQUENCE_NUMBER);

    private final String apiValue;
    private final String[] entityValue;

    ApplicationListEntriesSummarySortFieldEnum(String apiValue, String... entityValue) {
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
