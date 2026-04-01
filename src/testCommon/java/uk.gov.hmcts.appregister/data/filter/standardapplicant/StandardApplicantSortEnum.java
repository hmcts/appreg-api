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
                                    keyable.setApplicantCode(
                                            PrimitiveDataGenerator.generate(count, 10));
                                }
                            })
                    .build());
    // TODO: Fix this test. Sort is not correct
    /*
        TITLE(
                SortMetaDataDescriptor.<StandardApplicant>builder()
                        .sortableOperationEnum(StandardApplicantSortFieldEnum.NAME)
                        .sortableValueFunction(keyable -> {
                            String name = "";
                            String forename = "";
                            String surname = "";

                            if (keyable.getName() != null) {
                                name = keyable.getName();
                            }
                            if (keyable.getApplicantForename1() != null) {
                                forename = keyable.getApplicantForename1();
                            }

                            if (keyable.getApplicantSurname() != null) {
                                surname = keyable.getApplicantSurname();
                            }
                            return name + " " + forename + " " + surname;
                        })
                        .sortGenerator(
                                new GenerateAccordingToSort<StandardApplicant>() {
                                    @Override
                                    public void apply(
                                            int count,
                                            StandardApplicant keyable,
                                            SortMetaDataDescriptor<StandardApplicant> descriptor) {
                                        if (count % 2 == 0) {
                                            keyable.setName(PrimitiveDataGenerator.generate(count, 35));
                                            keyable.setApplicantSurname(null);
                                            keyable.setApplicantForename1(null);
                                        } else {
                                            keyable.setApplicantForename1(PrimitiveDataGenerator.generate(count, 35));
                                            keyable.setApplicantSurname(PrimitiveDataGenerator.generate(count, 35));
                                            keyable.setName(null);
                                        }
                                    }
                                })
                        .build());
    */
    private SortMetaDataDescriptor<StandardApplicant> sortDataDescriptor;

    StandardApplicantSortEnum(SortMetaDataDescriptor<StandardApplicant> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<StandardApplicant> getDescriptor() {
        return sortDataDescriptor;
    }
}
