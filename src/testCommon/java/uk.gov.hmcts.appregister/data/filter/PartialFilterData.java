package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class PartialFilterData extends FilterFieldData {
    /** The partial value. */
    private final String startsWith;

    private final String middleWith;

    private final String endsWith;
}
