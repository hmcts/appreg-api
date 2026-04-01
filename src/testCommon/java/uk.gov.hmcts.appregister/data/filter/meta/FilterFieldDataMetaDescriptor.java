package uk.gov.hmcts.appregister.data.filter.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;

/**
 * Describes what data we want to generate in order to drive the filter functionality.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FilterFieldDataMetaDescriptor<T extends Keyable> {
    /** The filer name. */
    private String queryName;

    /** Should we support partial matches. */
    private boolean partialSupport;

    /** Is this field a case insensitive filter. */
    private boolean caseInsensitive;

    /** Sets the value on the keyable according to the filter metadata. */
    private GenerateAccordingToFilter<T> filterGenerator;

    /**
     * Apply the filter to the keyable.
     *
     * @param count The record number of the filter field data.
     * @param keyable The keyable to apply the filter to.
     * @return The filter field data that is generated according to the filter metadata.
     */
    public FilterFieldData<T> apply(int count, T keyable) {
        return filterGenerator.apply(count, keyable, this);
    }
}
