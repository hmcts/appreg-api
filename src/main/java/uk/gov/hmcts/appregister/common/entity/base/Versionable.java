package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.EntityListeners;

import java.math.BigDecimal;

/** Interface to be implemented by entities that require versioning. */
@EntityListeners(PreCreateUpdateEntityListener.class)
public interface Versionable {
    void setVersion(Long version);

    Long getVersion();
}
