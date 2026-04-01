package uk.gov.hmcts.appregister.applicationentry.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntrySortFieldEnum implements SortableOperationEnum {
    // ensure we always add id to guarantee a consistent sort order in searches
    DATE("date", "id", "date"),
    APPLICANT("applicant", "id", "applicantName"),
    RESPONDENT("respondent", "id", "respondentName"),
    APPLICATION_TITLE("applicationTitle", "id", "applicationTitle"),
    FEE_REQUIRED("feeRequired", "id", "feeRequired"),
    RESULTED("resulted", "id", "resulted"),
    STATUS("status", "id", "status");

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
