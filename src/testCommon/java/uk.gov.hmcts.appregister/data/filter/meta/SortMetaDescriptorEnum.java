package uk.gov.hmcts.appregister.data.filter.meta;

/**
 * A sort meta descriptor enum. This is needed to work with the {@link
 * uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory}. Each enum value describes a sort
 * meta descriptor and how that sort value can be set on the keyable.
 */
public interface SortMetaDescriptorEnum<T> {
    SortMetaDataDescriptor<T> getDescriptor();
}
