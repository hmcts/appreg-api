package uk.gov.hmcts.appregister.courtlocation.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse_;

@Getter
public enum CourtLocationSortFieldMapper implements SortableOperationEnum {
    TITLE("name", NationalCourtHouse_.ID, NationalCourtHouse_.NAME),
    CODE("code", NationalCourtHouse_.ID, NationalCourtHouse_.COURT_LOCATION_CODE);

    private static final Map<String, SortableOperationEnum> MAPPINGS = new HashMap<>();

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    CourtLocationSortFieldMapper(String apiValue, String tieBreaker, String... entityValue) {
        this.apiValue = apiValue;
        this.entityValue = entityValue;
        this.tieBreaker = tieBreaker;
    }

    static {
        for (SortableOperationEnum status : values()) {
            MAPPINGS.put(status.getApiValue(), status);
        }
    }

    public static SortableOperationEnum getEntityValue(String apiValue) {
        return MAPPINGS.get(apiValue);
    }
}
