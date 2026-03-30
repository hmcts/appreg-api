package uk.gov.hmcts.appregister.data.filter.standardapplicant;

import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;

/**
 * An enumeration that allows us to setup sort for the standard applicant.
 */
public enum StandardApplicantSortEnum implements SortMetaDescriptorEnum<StandardApplicant> {
    CODE(
            SortMetaDataDescriptor.<StandardApplicant>builder()
                    .sortableOperationEnum(StandardApplicantSortFieldEnum.CODE)
                    .sortableValueFunction(StandardApplicant::getApplicantCode)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<StandardApplicant>() {
                                @Override
                                public void apply(
                                        int count,
                                        StandardApplicant keyable,
                                        SortMetaDataDescriptor<StandardApplicant> descriptor) {
                                    keyable.setApplicantCode(PrimitiveDataGenerator.generate(10));
                                }
                            })
                    .build()),

    TITLE(
            SortMetaDataDescriptor.<StandardApplicant>builder()
                    .sortableOperationEnum(StandardApplicantSortFieldEnum.NAME)
                    .sortableValueFunction(StandardApplicant::getApplicantTitle)
                    .sortGenerator(
                            new GenerateAccordingToSort<StandardApplicant>() {
                                @Override
                                public void apply(
                                        int count,
                                        StandardApplicant keyable,
                                        SortMetaDataDescriptor<StandardApplicant> descriptor) {
                                    keyable.setName(PrimitiveDataGenerator.generate(35));
                                }
                            })
                    .build());

    private SortMetaDataDescriptor<StandardApplicant> sortDataDescriptor;

    StandardApplicantSortEnum(SortMetaDataDescriptor<StandardApplicant> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<StandardApplicant> getDescriptor() {
        return sortDataDescriptor;
    }
}
