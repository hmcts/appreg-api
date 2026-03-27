package uk.gov.hmcts.appregister.data.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToFilter;

/**
 * Describes what data we want to generate in order to drive the filter functionality.
 */
@Builder
@Getter
@AllArgsConstructor
public class FilterFieldDataDescriptor<T extends Keyable> {
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
    private GenerateAccordingToFilter<T> filterGenerator;

    public FilterFieldDataDescriptor() {}

    /**
     * Apply the filter to the keyable.
     */
    public FilterFieldData<T> apply(T keyable, OrderEnum orderEnum) {
        return filterGenerator.apply(keyable, this, orderEnum);
    }
}
