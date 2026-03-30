package uk.gov.hmcts.appregister.data.filter.standardapplicant;

import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;

public enum StandardApplicantSortEnum implements SortDescriptorEnum<StandardApplicant> {

    CODE(SortDataDescriptor.<StandardApplicant>builder()
        .sortableOperationEnum(StandardApplicantSortFieldEnum.CODE)
        .sortableValueFunction(StandardApplicant::getApplicantCode).defaultSort(true)
        .sortGenerator(new GenerateAccordingToSort<StandardApplicant>() {
            @Override
            public void apply(int count, StandardApplicant keyable, SortDataDescriptor<StandardApplicant> descriptor) {
                keyable.setApplicantCode(PrimitiveDataGenerator.generate(10));
            }
        }).build()),

    TITLE(SortDataDescriptor.<StandardApplicant>builder()
              .sortableOperationEnum(StandardApplicantSortFieldEnum.NAME)
              .sortableValueFunction(StandardApplicant::getApplicantTitle)
              .sortGenerator(new GenerateAccordingToSort<StandardApplicant>() {
                  @Override
                  public void apply(int count, StandardApplicant keyable, SortDataDescriptor<StandardApplicant> descriptor) {
                      keyable.setName(PrimitiveDataGenerator.generate(35));
                  }
              }).build());

    private SortDataDescriptor<StandardApplicant> sortDataDescriptor;

    StandardApplicantSortEnum(SortDataDescriptor<StandardApplicant> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<StandardApplicant> getDescriptor() {
        return sortDataDescriptor;
    }


}
