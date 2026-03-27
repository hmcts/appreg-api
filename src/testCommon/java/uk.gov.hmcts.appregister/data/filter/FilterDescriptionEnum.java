package uk.gov.hmcts.appregister.data.filter;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

public interface FilterDescriptionEnum<T extends Keyable> {
    FilterFieldDataDescriptor<T> getDescriptor();
}
