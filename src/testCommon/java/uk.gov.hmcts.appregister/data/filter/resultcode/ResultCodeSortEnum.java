package uk.gov.hmcts.appregister.data.filter.resultcode;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.resultcode.api.ResultCodeSortFieldEnum;

public enum ResultCodeSortEnum implements SortDescriptorEnum<ResolutionCode> {

    CODE(SortDataDescriptor.<ResolutionCode>builder()
        .sortableOperationEnum(ResultCodeSortFieldEnum.CODE)
        .sortableValueFunction(ResolutionCode::getResultCode).defaultSort(true)
        .sortGenerator(new GenerateAccordingToSort<ResolutionCode>() {
            @Override
            public void apply(int count, ResolutionCode keyable, SortDataDescriptor<ResolutionCode> descriptor) {
                keyable.setResultCode(PrimitiveDataGenerator.generate(2));
            }
        }).build()),
    TITLE(SortDataDescriptor.<ResolutionCode>builder()
              .sortableOperationEnum(ResultCodeSortFieldEnum.TITLE)
              .sortableValueFunction(ResolutionCode::getTitle)
              .sortGenerator(new GenerateAccordingToSort<ResolutionCode>() {
                  @Override
                  public void apply(int count, ResolutionCode keyable, SortDataDescriptor<ResolutionCode> descriptor) {
                      keyable.setTitle(PrimitiveDataGenerator.generate(35));
                  }
              }).build());

    private SortDataDescriptor<ResolutionCode> sortDataDescriptor;

    ResultCodeSortEnum(SortDataDescriptor<ResolutionCode> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<ResolutionCode> getDescriptor() {
        return sortDataDescriptor;
    }


}
