package uk.gov.hmcts.appregister.applicationentry.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ApplicationEntrySortFieldEnum implements SortableOperationEnum {


    DATE("date", "date"),
    CODE("courtCode", "courtCode"),
    LOCATION("otherLocationDescription", "otherLocationDescription"),
    CJA_CODE("cjaCode", "cjaCode"),
    APPLICANT_ORG("applicantOrganisation", "applicantOrganisation"),
    APPLICANT_SURNAME("applicantSurname", "applicantSurname"),
    APPLICANT_CODE("standardApplicantCode", "standardApplicantCode"),
    RESPONDENT_ORG("respondentOrganisation", "respondentOrganisation"),
    RESPONDENT_SURNAME("respondentSurname", "respondentSurname"),
    RESPONENT_POSTCODE("respondentPostcode", "respondentPostcode"),
    ACCOUNT_REFERENCE("accountReference", "accountReference"),
    STATUS("status", "status");

    private final String apiValue;
    private final String entityValue;

    ApplicationEntrySortFieldEnum(String apiValue, String entityValue) {
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
