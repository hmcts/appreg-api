package uk.gov.hmcts.appregister.data.filter.courtlocation;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.courtlocation.api.CourtLocationSortFieldMapper;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup sort for the court justice endpoint.
 */
public enum CourtLocationSortEnum implements SortMetaDescriptorEnum<NationalCourtHouse> {
    CODE(
            SortMetaDataDescriptor.<NationalCourtHouse>builder()
                    .sortableOperationEnum(CourtLocationSortFieldMapper.CODE)
                    .sortableValueFunction(NationalCourtHouse::getCourtLocationCode)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<NationalCourtHouse>() {
                                @Override
                                public void apply(
                                        int count,
                                        NationalCourtHouse keyable,
                                        SortMetaDataDescriptor<NationalCourtHouse> descriptor) {
                                    keyable.setCourtLocationCode(
                                            PrimitiveDataGenerator.generate(count, 10));
                                }
                            })
                    .build()),
    TITLE(
            SortMetaDataDescriptor.<NationalCourtHouse>builder()
                    .sortableOperationEnum(CourtLocationSortFieldMapper.TITLE)
                    .sortableValueFunction(NationalCourtHouse::getName)
                    .sortGenerator(
                            new GenerateAccordingToSort<NationalCourtHouse>() {
                                @Override
                                public void apply(
                                        int count,
                                        NationalCourtHouse keyable,
                                        SortMetaDataDescriptor<NationalCourtHouse> descriptor) {
                                    keyable.setName(PrimitiveDataGenerator.generate());
                                }
                            })
                    .build());

    private SortMetaDataDescriptor<NationalCourtHouse> sortDataDescriptor;

    CourtLocationSortEnum(SortMetaDataDescriptor<NationalCourtHouse> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<NationalCourtHouse> getDescriptor() {
        return sortDataDescriptor;
    }
}
