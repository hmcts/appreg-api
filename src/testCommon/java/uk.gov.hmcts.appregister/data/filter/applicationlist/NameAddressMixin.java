package uk.gov.hmcts.appregister.data.filter.applicationlist;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A jackson mixin that we can use to ignore serialization and deserialization of the {@link
 * uk.gov.hmcts.appregister.common.entity.NameAddress} class.
 */
public abstract class NameAddressMixin {
    @JsonIgnore
    abstract boolean isApplicant();

    @JsonIgnore
    abstract boolean isRespondent();
}
