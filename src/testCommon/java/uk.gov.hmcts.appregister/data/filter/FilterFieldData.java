package uk.gov.hmcts.appregister.data.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Maps the keyable data to a generic object.
 */
@Setter
@Getter
@Builder
public class FilterFieldData {
    private final FilterFieldDataDescriptor descriptor;
    private final FilterValue keyableValues;
}
