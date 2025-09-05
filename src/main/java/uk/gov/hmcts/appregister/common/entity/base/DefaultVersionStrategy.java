package uk.gov.hmcts.appregister.common.entity.base;

import org.springframework.stereotype.Component;

/**
 * A basic implementation of the versioning that simply increments the version number by 1 each
 * time.
 */
@Component
public class DefaultVersionStrategy implements VersionStrategy {
    @Override
    public void updateVersion(Versionable versionable) {
        if (versionable.getVersion() == null) {
            versionable.setVersion(0L);
        }

        // increment the version
        versionable.setVersion(versionable.getVersion() + 1);
    }
}
