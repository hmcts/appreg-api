package uk.gov.hmcts.appregister.common.util;

import java.util.AbstractList;
import java.util.List;

/**
 * Implements a delegate pattern to prevent modification of the underlying list.
 */
public class ReadOnlyList<E> extends AbstractList<E> {
    /** The underlying list will be exposed if extended or default */
    protected final List<E> backing;

    public ReadOnlyList(List<E> source) {
        this.backing = source; // immutable copy
    }

    @Override
    public E get(int index) {
        return backing.get(index);
    }

    @Override
    public int size() {
        return backing.size();
    }
}
