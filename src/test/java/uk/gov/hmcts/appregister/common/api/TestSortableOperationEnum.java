package uk.gov.hmcts.appregister.common.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Defines the API sort field names exposed by the Application Code endpoint and the associated
 * mapping onto the backend database.
 *
 * <p>These constants represent the property names that clients/backend can use in sorting
 */
@Getter
public enum TestSortableOperationEnum implements SortableOperationEnum {
    TEST_NO_TIE_BREAKER("test1Api", null, "test1Entity"),
    TEST2_NO_TIE_BREAKER("test2Api", null, "test2Entity"),
    TEST_TIE_BREAKER("test1ApiTB", "tie", "test1Entity"),
    TEST2_TIE_BREAKER("test2ApiTB", "tie", "test2Entity");

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    TestSortableOperationEnum(String apiValue, String tieBreaker, String... entityValue) {
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
