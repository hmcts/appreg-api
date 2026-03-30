package uk.gov.hmcts.appregister.data.filter.applicationlist;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class NameAddressMixin {
    @JsonIgnore abstract boolean isApplicant();
    @JsonIgnore abstract boolean isRespondent();

}
