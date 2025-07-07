package uk.gov.hmcts.appregister.service;

import org.springframework.stereotype.Component;

@Component
public class VersionManager {
    public int increment(Integer currentVersion) {
        if (currentVersion == null) {
            return 1;
        }
        return currentVersion + 1;
    }
}
