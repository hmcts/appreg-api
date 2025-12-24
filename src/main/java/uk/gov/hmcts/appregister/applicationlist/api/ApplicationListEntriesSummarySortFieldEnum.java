package uk.gov.hmcts.appregister.applicationlist.api;

import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the API field names exposed by the Application List endpoints.
 *
 * <p>These constants represent the property names that clients can use in filters, sorting, or
 * query parameters.
 */
public final class ApplicationListEntriesSortFieldEnum  implements SortableOperationEnum {
    DATE("date",ApplicationList_.DATE, "id"),
    CODE("code",ApplicationCode_.CODE),
    TIME("time", ApplicationList_.TIME),
    STATUS("status", ApplicationList_.STATUS),
    LOCATION("courtLocationCode", ApplicationList_.COURT_CODE),
    CJA("cja", ApplicationList_.CJA),
    DESCRIPTION("description", ApplicationList_.DESCRIPTION),
    OTHER("otherLocationDescription", ApplicationList_.OTHER_LOCATION);

    private final String apiValue;
    private final String[] entityValue;

    ApplicationListSortFieldEnum(String apiValue, String... entityValue) {
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
