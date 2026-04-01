package uk.gov.hmcts.appregister.data.filter.applicationlist;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A jackson mixin that we can use to ignore serialization and deserialization of the {@link
 * uk.gov.hmcts.appregister.common.entity.ApplicationList} class.
 */
public abstract class ApplicationListMixin {
    @JsonIgnore
    abstract boolean isOpen();
}
