package uk.gov.hmcts.appregister.standardapplicant.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant_;

/**
 * Defines the API sort field names exposed by the Standard Applicant endpoint and the associated
 * mapping onto the backend database.
 *
 * <p>These constants represent the property names that clients/backend can use in sorting
 */
@Getter
public enum StandardApplicantSortFieldEnum implements SortableOperationEnum {
    NAME("name", StandardApplicant_.ID, "effectiveName"),
    CODE("code", StandardApplicant_.APPLICANT_CODE),
    ADDRESS_LINE_1("addressLine1", StandardApplicant_.ID, StandardApplicant_.ADDRESS_LINE1),
    FROM("from", StandardApplicant_.ID, StandardApplicant_.APPLICANT_START_DATE),
    TO("to", StandardApplicant_.ID, StandardApplicant_.APPLICANT_END_DATE);

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    StandardApplicantSortFieldEnum(String apiValue, String tieBreaker, String... entityValue) {
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
