package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup sort for the application code endpoint.
 */
public enum ApplicationCodeSortEnum implements SortMetaDescriptorEnum<ApplicationCode> {
    CODE(
            SortMetaDataDescriptor.<ApplicationCode>builder()
                    .sortableOperationEnum(ApplicationCodeSortFieldEnum.CODE)
                    .sortableValueFunction(ApplicationCode::getCode)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationCode>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationCode keyable,
                                        SortMetaDataDescriptor<ApplicationCode> descriptor) {
                                    keyable.setCode(PrimitiveDataGenerator.generate(10));
                                }
                            })
                    .build()),
    TITLE(
            SortMetaDataDescriptor.<ApplicationCode>builder()
                    .sortableOperationEnum(ApplicationCodeSortFieldEnum.TITLE)
                    .sortableValueFunction(ApplicationCode::getTitle)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationCode>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationCode keyable,
                                        SortMetaDataDescriptor<ApplicationCode> descriptor) {
                                    keyable.setTitle(PrimitiveDataGenerator.generate());
                                }
                            })
                    .build());

    private SortMetaDataDescriptor<ApplicationCode> sortDataDescriptor;

    ApplicationCodeSortEnum(SortMetaDataDescriptor<ApplicationCode> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<ApplicationCode> getDescriptor() {
        return sortDataDescriptor;
    }
}
