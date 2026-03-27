package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.value.AbstractSortGenerator;
import uk.gov.hmcts.appregister.data.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.sort.SortDescriptorEnum;

import java.util.Random;

public enum ApplicationCodeSortEnum implements SortDescriptorEnum<ApplicationCode> {

    CODE(SortDataDescriptor.<ApplicationCode>builder()
        .sortableOperationEnum(ApplicationCodeSortFieldEnum.CODE)
        .sortableValueFunction(ApplicationCode::getCode).defaultSort(true)
        .sortGenerator(new AbstractSortGenerator<ApplicationCode>() {
            @Override
            public void apply(ApplicationCode keyable, SortDataDescriptor<ApplicationCode> descriptor, OrderEnum orderEnum) {
                keyable.setCode(getString(orderEnum, 10));
            }
        }).build()),
    TITLE(SortDataDescriptor.<ApplicationCode>builder()
              .sortableOperationEnum(ApplicationCodeSortFieldEnum.TITLE)
              .sortableValueFunction(ApplicationCode::getTitle)
              .sortGenerator(new AbstractSortGenerator<ApplicationCode>() {
                  @Override
                  public void apply(ApplicationCode keyable, SortDataDescriptor<ApplicationCode> descriptor, OrderEnum orderEnum) {
                      keyable.setTitle(getString(orderEnum, null));
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
