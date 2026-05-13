package uk.gov.hmcts.appregister.applicationentry.api;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

@Getter
public enum ApplicationEntryDefaultSortFieldEnum implements SortableOperationEnum {
    CODE("courtCode", "id", "courtCode");

    private final String apiValue;
    private final String[] entityValue;
    private final String tieBreaker;

    ApplicationEntryDefaultSortFieldEnum(
            String apiValue, String tieBreaker, String... entityValue) {
        this.apiValue = apiValue;
        this.entityValue = entityValue;
        this.tieBreaker = tieBreaker;
    }
}
