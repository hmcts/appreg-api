package uk.gov.hmcts.appregister.common.entity.base;

/** This interface is to be implemented by entities that need to track the user who created them. */
public interface Accountable {

    /**
     * Gets the username of the user who created the entity.
     *
     * @return The username of the user who created the entity.
     */
    String getCreatedUser();

    /**
     * sets the username of the user who created the entity.
     *
     * @param user The username of the user who created the entity.
     */
    void setCreatedUser(String user);
}
