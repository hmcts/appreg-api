package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;

@FunctionalInterface
public interface GenerateAccordingToFilter {
    FilterFieldData apply(Keyable keyable, FilterFieldDataDescriptor descriptor, OrderEnum orderEnum);
}
