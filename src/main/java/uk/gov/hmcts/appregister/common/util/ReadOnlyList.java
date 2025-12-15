package uk.gov.hmcts.appregister.common.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Implements a delegate pattern to prevent modification of the underlying list.
 */
public class ReadOnlyList<E> extends AbstractList<E> {
    /** The underlying list will be exposed if extended or default. */
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

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new IllegalArgumentException("Cannot replace read-only list");
    }

    @Override
    public void addFirst(E e) {
        throw new IllegalArgumentException("Cannot add read-only list");
    }

    @Override
    public void addLast(E e) {
        throw new IllegalArgumentException("Cannot add read-only list");
    }

    @Override
    public E removeFirst() {
        throw new IllegalArgumentException("Cannot remove read-only list");
    }

    @Override
    public E removeLast() {
        throw new IllegalArgumentException("Cannot remove read-only list");
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new IllegalArgumentException("Cannot remove read-only list");
    }
}
