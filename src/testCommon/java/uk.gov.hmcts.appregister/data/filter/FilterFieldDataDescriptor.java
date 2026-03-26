package uk.gov.hmcts.appregister.data.filter;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToFilter;

/**
 * Describes what data we want to generate in order to drive the filter functionality.
 */
@Builder
@Getter
public class FilterFieldDataDescriptor {
    /**
     * The column name that we want to filter on.
     */
    private String queryName;

    /** Should we support partial matches. */
    private boolean partialSupport;

    /** Is this field a case insensitive filter. */
    private boolean caseInsensitive;

    /**
     * Sets the value on the keyable according to the filter.
     */
    private GenerateAccordingToFilter filterGenerator;

    /**
     * Apply the filter to the keyable.
     */
    public FilterFieldData apply(Keyable keyable, OrderEnum orderEnum) {
        filterGenerator.apply(keyable, this, orderEnum);
    }


}
