package uk.gov.hmcts.appregister.data.filter.meta;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * A filter meta descriptor enum. This is needed to work with the {@link
 * uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory}. Each enum value describes a filter
 * meta descriptor and how that filter value can be set on the keyable.
 */
public interface FilterMetaDescriptorEnum<T extends Keyable> {
    FilterFieldDataMetaDescriptor<T> getDescriptor();
}
