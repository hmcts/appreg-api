package uk.gov.hmcts.appregister.data.filter.applicationlist;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ApplicationListMixin {
    @JsonIgnore abstract boolean isOpen();

}
