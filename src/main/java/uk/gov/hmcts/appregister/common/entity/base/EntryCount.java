package uk.gov.hmcts.appregister.common.entity.base;

import java.util.UUID;

public interface EntryCount {

    UUID getPrimaryKey();

    Long getCount();
}
