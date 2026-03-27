package uk.gov.hmcts.appregister.data.sort;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.Comparator;

/**
 * Sorts a list of keyable objects based on the sort descriptor column
 * against a keyable.
 */
public class KeyableSortComparator implements Comparator<Keyable> {
    private SortDataDescriptor descriptor;

    public int compare(Keyable o1, Keyable o2) {
        // push nulls to the end of the list
        if (o1 == null || o2 == null) {
            return -1;
        }

        Object value = descriptor.sortableValueFunction.apply(o1);
        Object value2 = descriptor.sortableValueFunction.apply(o2);

        // simulate the tie breaker if the values are equal. The ids should never be
        if (value.equals(value2)) {
            o1.getId().compareTo(o2.getId());
        }

        return ((Comparable<Object>)value).compareTo(value2);
    }

    /**
     * Only one sort field is supported.
     * @param descriptor The descriptor to set the sort field to.
     */
    public void setSortField(SortDataDescriptor descriptor) {
        this.descriptor = descriptor;
    }
}
