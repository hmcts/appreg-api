package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;

public enum ApplicationCodeSortEnum implements SortDescriptorEnum<ApplicationCode> {

    CODE(SortDataDescriptor.<ApplicationCode>builder()
        .sortableOperationEnum(ApplicationCodeSortFieldEnum.CODE)
        .sortableValueFunction(ApplicationCode::getCode).defaultSort(true)
        .sortGenerator(new GenerateAccordingToSort<ApplicationCode>() {
            @Override
            public void apply(int count, ApplicationCode keyable, SortDataDescriptor<ApplicationCode> descriptor) {
                keyable.setCode(PrimitiveDataGenerator.generate(10));
            }
        }).build()),
    TITLE(SortDataDescriptor.<ApplicationCode>builder()
              .sortableOperationEnum(ApplicationCodeSortFieldEnum.TITLE)
              .sortableValueFunction(ApplicationCode::getTitle)
              .sortGenerator(new GenerateAccordingToSort<ApplicationCode>() {
                  @Override
                  public void apply(int count, ApplicationCode keyable, SortDataDescriptor<ApplicationCode> descriptor) {
                      keyable.setTitle(PrimitiveDataGenerator.generate());
                  }
              }).build());

    private SortDataDescriptor<ApplicationCode> sortDataDescriptor;

    ApplicationCodeSortEnum(SortDataDescriptor<ApplicationCode> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<ApplicationCode> getDescriptor() {
        return sortDataDescriptor;
    }


}
