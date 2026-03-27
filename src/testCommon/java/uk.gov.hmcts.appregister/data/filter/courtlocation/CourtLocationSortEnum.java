package uk.gov.hmcts.appregister.data.filter.courtlocation;

import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.courtlocation.api.CourtLocationSortFieldMapper;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.value.AbstractSortGenerator;
import uk.gov.hmcts.appregister.data.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.sort.SortDescriptorEnum;

public enum CourtLocationSortEnum implements SortDescriptorEnum<NationalCourtHouse> {

    CODE(SortDataDescriptor.<NationalCourtHouse>builder()
        .sortableOperationEnum(CourtLocationSortFieldMapper.CODE)
        .sortableValueFunction(NationalCourtHouse::getCourtLocationCode).defaultSort(true)
        .sortGenerator(new AbstractSortGenerator<NationalCourtHouse>() {
            @Override
            public void apply(NationalCourtHouse keyable, SortDataDescriptor<NationalCourtHouse> descriptor, OrderEnum orderEnum) {
                keyable.setCourtLocationCode(getString(orderEnum, 10));
            }
        }).build()),
    TITLE(SortDataDescriptor.<NationalCourtHouse>builder()
              .sortableOperationEnum(CourtLocationSortFieldMapper.TITLE)
              .sortableValueFunction(NationalCourtHouse::getName)
              .sortGenerator(new AbstractSortGenerator<NationalCourtHouse>() {
                  @Override
                  public void apply(NationalCourtHouse keyable, SortDataDescriptor<NationalCourtHouse> descriptor, OrderEnum orderEnum) {
                      keyable.setName(getString(orderEnum, null));
                  }
              }).build());

    private SortDataDescriptor<NationalCourtHouse> sortDataDescriptor;

    CourtLocationSortEnum(SortDataDescriptor<NationalCourtHouse> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<NationalCourtHouse> getDescriptor() {
        return sortDataDescriptor;
    }


}
