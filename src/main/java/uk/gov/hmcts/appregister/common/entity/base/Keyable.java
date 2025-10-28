package uk.gov.hmcts.appregister.common.entity.base;

/**
 * An interface across all entities that have a primary key of type Long.
 */
public interface Keyable {

    /**
     * gets the id
     * @return The id
     */
    Long getId();
}
