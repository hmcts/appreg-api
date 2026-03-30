package uk.gov.hmcts.appregister.data.filter.courtlocation;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.courtlocation.api.CourtLocationSortFieldMapper;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;

public enum CourtLocationSortEnum implements SortDescriptorEnum<NationalCourtHouse> {

    CODE(SortDataDescriptor.<NationalCourtHouse>builder()
        .sortableOperationEnum(CourtLocationSortFieldMapper.CODE)
        .sortableValueFunction(NationalCourtHouse::getCourtLocationCode).defaultSort(true)
        .sortGenerator(new GenerateAccordingToSort<NationalCourtHouse>() {
            @Override
            public void apply(int count, NationalCourtHouse keyable, SortDataDescriptor<NationalCourtHouse> descriptor) {
                keyable.setCourtLocationCode(PrimitiveDataGenerator.generate(10));
            }
        }).build()),
    TITLE(SortDataDescriptor.<NationalCourtHouse>builder()
              .sortableOperationEnum(CourtLocationSortFieldMapper.TITLE)
              .sortableValueFunction(NationalCourtHouse::getName)
              .sortGenerator(new GenerateAccordingToSort<NationalCourtHouse>() {
                  @Override
                  public void apply(int count, NationalCourtHouse keyable, SortDataDescriptor<NationalCourtHouse> descriptor) {
                      keyable.setName(PrimitiveDataGenerator.generate());
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
