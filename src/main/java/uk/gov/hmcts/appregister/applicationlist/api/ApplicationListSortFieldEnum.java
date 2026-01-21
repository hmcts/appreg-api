package uk.gov.hmcts.appregister.applicationlist.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;

/**
 * Defines the API field names exposed by the Application List endpoints.
 *
 * <p>These constants represent the property names that clients can use in filters, sorting, or
 * query parameters.
 */
@Getter
public enum ApplicationListSortFieldEnum implements SortableOperationEnum {
    DATE("date", ApplicationList_.ID, ApplicationList_.DATE),
    TIME("time", ApplicationList_.ID, ApplicationList_.TIME),
    STATUS("status", ApplicationList_.ID, ApplicationList_.STATUS),
    LOCATION("courtLocationCode", ApplicationList_.ID, ApplicationList_.COURT_CODE),
    DESCRIPTION("description", ApplicationList_.ID, ApplicationList_.DESCRIPTION),
    OTHER_LOCATION(
            "otherLocationDescription", ApplicationList_.ID, ApplicationList_.OTHER_LOCATION);

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    ApplicationListSortFieldEnum(String apiValue, String tieBreaker, String... entityValue) {
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
