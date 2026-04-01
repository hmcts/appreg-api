package uk.gov.hmcts.appregister.data.filter.sort;

import java.util.Comparator;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;

/**
 * Sorts a list of keyable objects based on the sort descriptor {@link SortMetaDataDescriptor}
 * column against a keyable.
 */
public class KeyableSortComparator<T extends Keyable> implements Comparator<T> {
    private SortMetaDataDescriptor<T> sortDesciptor;

    public int compare(T o1, T o2) {
        // push nulls to the end of the list
        if (o1 == null || o2 == null) {
            return -1;
        }

        Object value = sortDesciptor.getSortableValueFunction().apply(o1);
        Object value2 = sortDesciptor.getSortableValueFunction().apply(o2);

        // simulate the tie breaker if the values are equal. The ids should never be
        if (value.equals(value2)) {
            return o1.getId().compareTo(o2.getId());
        }

        // if the value implements comparable, use it.
        if (value instanceof Comparable valCompare) {
            return valCompare.compareTo(value2);
        }

        return 0;
    }

    /**
     * set the comparable to sort according to the descriptor.
     *
     * @param descriptor The descriptor to sort .
     */
    public void setSortDescriptor(SortMetaDataDescriptor<T> descriptor) {
        this.sortDesciptor = descriptor;
    }
}
