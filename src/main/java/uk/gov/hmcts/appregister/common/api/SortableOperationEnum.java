package uk.gov.hmcts.appregister.common.api;

/**
 * An enumeration entry for an operation. The instance maps the API sort fields (using the client)
 * to the entity sort fields (used in the database).
 *
 * <p>This enum provides a useful mapping between the two representations that is specific to an
 * operation.
 */
public interface SortableOperationEnum {
    /** The external facing API values. */
    String getApiValue();

    /** The backend entity value. */
    String[] getEntityValue();
}
