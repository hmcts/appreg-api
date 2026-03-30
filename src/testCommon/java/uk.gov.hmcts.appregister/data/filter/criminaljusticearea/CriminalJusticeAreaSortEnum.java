package uk.gov.hmcts.appregister.data.filter.criminaljusticearea;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.criminaljusticearea.api.CriminalJusticeSortFieldEnum;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;

public enum CriminalJusticeAreaSortEnum implements SortDescriptorEnum<CriminalJusticeArea> {

    CODE(SortDataDescriptor.<CriminalJusticeArea>builder()
        .sortableOperationEnum(CriminalJusticeSortFieldEnum.CODE)
        .sortableValueFunction(CriminalJusticeArea::getCode).defaultSort(true)
        .sortGenerator(new GenerateAccordingToSort<CriminalJusticeArea>() {
            @Override
            public void apply(int count, CriminalJusticeArea keyable, SortDataDescriptor<CriminalJusticeArea> descriptor) {
                keyable.setCode(PrimitiveDataGenerator.generate(2));
            }
        }).build()),
    TITLE(SortDataDescriptor.<CriminalJusticeArea>builder()
              .sortableOperationEnum(CriminalJusticeSortFieldEnum.DESCRIPTION)
              .sortableValueFunction(CriminalJusticeArea::getDescription)
              .sortGenerator(new GenerateAccordingToSort<CriminalJusticeArea>() {
                  @Override
                  public void apply(int count, CriminalJusticeArea keyable, SortDataDescriptor<CriminalJusticeArea> descriptor) {
                      keyable.setDescription(PrimitiveDataGenerator.generate(35));
                  }
              }).build());

    private SortDataDescriptor<CriminalJusticeArea> sortDataDescriptor;

    CriminalJusticeAreaSortEnum(SortDataDescriptor<CriminalJusticeArea> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<CriminalJusticeArea> getDescriptor() {
        return sortDataDescriptor;
    }


}
