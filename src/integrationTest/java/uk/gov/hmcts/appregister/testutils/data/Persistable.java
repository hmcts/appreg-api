package uk.gov.hmcts.appregister.testutils.data;

/**
 * Interface for creating persistable entities with minimal and maximal field population.
 *
 * <p>Implementations should ensure that the objects returned by these methods can be persisted
 * without violating any non-null constraints, except for the id field which is expected to be
 * populated by the persistence layer upon save.
 *
 * @param <T> the type of the entity
 */
public interface Persistable<T> {

    /**
     * Return an entity that has only its non-null fields populated. All other fields should be
     * expected to be null.
     *
     * <p>This represents the minimal persistable object that may be passed to an entityManager
     * without throwing any constraint violation exceptions.
     *
     * <p>NOTE: Objects created by this method will never populate the id field, as this is expected
     * to be populated by the persistence layer upon save.
     *
     * @return a minimally persistent instance of M
     */
    T someMinimal();

    /**
     * Return an entity that has all of its fields populated.
     *
     * <p>This should always be a superset of someMinimal(), and in cases where the entity has
     * non-null constraints on every field the implementation should be identical to someMinimal().
     *
     * <p>NOTE: Objects created by this method will never populate the id field, as this is expected
     * to be populated by the persistence layer upon save.
     *
     * @return a maximally persistent instance of M
     */
    default T someMaximal() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
