package uk.gov.hmcts.appregister.data.filter.resultcode;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.resultcode.api.ResultCodeSortFieldEnum;

/**
 * An enumeration that allows us to setup sort for the result code.
 */
public enum ResultCodeSortEnum implements SortMetaDescriptorEnum<ResolutionCode> {
    CODE(
            SortMetaDataDescriptor.<ResolutionCode>builder()
                    .sortableOperationEnum(ResultCodeSortFieldEnum.CODE)
                    .sortableValueFunction(ResolutionCode::getResultCode)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<ResolutionCode>() {
                                @Override
                                public void apply(
                                        int count,
                                        ResolutionCode keyable,
                                        SortMetaDataDescriptor<ResolutionCode> descriptor) {
                                    keyable.setResultCode(
                                            PrimitiveDataGenerator.generate(count, 2));
                                }
                            })
                    .build()),
    TITLE(
            SortMetaDataDescriptor.<ResolutionCode>builder()
                    .sortableOperationEnum(ResultCodeSortFieldEnum.TITLE)
                    .sortableValueFunction(ResolutionCode::getTitle)
                    .sortGenerator(
                            new GenerateAccordingToSort<ResolutionCode>() {
                                @Override
                                public void apply(
                                        int count,
                                        ResolutionCode keyable,
                                        SortMetaDataDescriptor<ResolutionCode> descriptor) {
                                    keyable.setTitle(PrimitiveDataGenerator.generate(count, 35));
                                }
                            })
                    .build());

    private SortMetaDataDescriptor<ResolutionCode> sortDataDescriptor;

    ResultCodeSortEnum(SortMetaDataDescriptor<ResolutionCode> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<ResolutionCode> getDescriptor() {
        return sortDataDescriptor;
    }
}
