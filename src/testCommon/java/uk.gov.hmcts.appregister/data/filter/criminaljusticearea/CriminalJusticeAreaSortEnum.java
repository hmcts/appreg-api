package uk.gov.hmcts.appregister.data.filter.criminaljusticearea;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.criminaljusticearea.api.CriminalJusticeSortFieldEnum;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup sort for the criminal justice endpoint.
 */
public enum CriminalJusticeAreaSortEnum implements SortMetaDescriptorEnum<CriminalJusticeArea> {
    CODE(
            SortMetaDataDescriptor.<CriminalJusticeArea>builder()
                    .sortableOperationEnum(CriminalJusticeSortFieldEnum.CODE)
                    .sortableValueFunction(CriminalJusticeArea::getCode)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<CriminalJusticeArea>() {
                                @Override
                                public void apply(
                                        int count,
                                        CriminalJusticeArea keyable,
                                        SortMetaDataDescriptor<CriminalJusticeArea> descriptor) {
                                    keyable.setCode(PrimitiveDataGenerator.generate(count, 2));
                                }
                            })
                    .build()),
    TITLE(
            SortMetaDataDescriptor.<CriminalJusticeArea>builder()
                    .sortableOperationEnum(CriminalJusticeSortFieldEnum.DESCRIPTION)
                    .sortableValueFunction(CriminalJusticeArea::getDescription)
                    .sortGenerator(
                            new GenerateAccordingToSort<CriminalJusticeArea>() {
                                @Override
                                public void apply(
                                        int count,
                                        CriminalJusticeArea keyable,
                                        SortMetaDataDescriptor<CriminalJusticeArea> descriptor) {
                                    keyable.setDescription(
                                            PrimitiveDataGenerator.generate(count, 35));
                                }
                            })
                    .build());

    private SortMetaDataDescriptor<CriminalJusticeArea> sortDataDescriptor;

    CriminalJusticeAreaSortEnum(SortMetaDataDescriptor<CriminalJusticeArea> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<CriminalJusticeArea> getDescriptor() {
        return sortDataDescriptor;
    }
}
