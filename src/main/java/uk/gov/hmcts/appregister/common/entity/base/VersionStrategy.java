package uk.gov.hmcts.appregister.common.entity.base;

/** An interface that allows us to define how the version id is incremented. */
public interface VersionStrategy {
    /**
     * The versionable entity is passed in so that the strategy can decide how to update the
     * version.
     *
     * @param versionable The versionable object that will be updated
     */
    void updateVersion(Versionable versionable);
}
