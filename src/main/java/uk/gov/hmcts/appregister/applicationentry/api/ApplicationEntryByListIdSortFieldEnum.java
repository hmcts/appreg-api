package uk.gov.hmcts.appregister.applicationentry.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntryByListIdSortFieldEnum implements SortableOperationEnum {
    SEQUENCE_NUMBER("sequenceNumber", "id", "sequenceNumber"),
    APPLICATION_TITLE("applicationTitle", "id", "applicationTitle"),
    APPLICANT("applicant", "id", "applicant"),
    RESPONDENT("respondent", "id", "respondent"),
    RESPONDENT_POSTCODE("respondentPostcode", "id", "respondentPostcode"),
    ACCOUNT_REFERENCE("accountReference", "id", "accountReference"),
    FEE_REQUIRED("feeRequired", "id", "feeRequired"),
    RESULTED("resulted", "id", "resulted");

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    ApplicationEntryByListIdSortFieldEnum(
            String apiValue, String tieBreaker, String... entityValue) {
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
