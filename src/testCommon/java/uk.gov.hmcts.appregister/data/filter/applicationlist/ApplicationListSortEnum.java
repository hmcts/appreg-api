package uk.gov.hmcts.appregister.data.filter.applicationlist;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup sort for the application list endpoint.
 */
public enum ApplicationListSortEnum implements SortMetaDescriptorEnum<ApplicationList> {
    DATE(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.DATE)
                    .sortableValueFunction(ApplicationList::getDate)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setDate(PrimitiveDataGenerator.getDate(count));
                                }
                            })
                    .build()),
    TIME(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.TIME)
                    .sortableValueFunction(ApplicationList::getTime)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setTime(PrimitiveDataGenerator.getTime(count));
                                }
                            })
                    .build()),
    STATUS(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.STATUS)
                    .sortableValueFunction(ApplicationList::getStatus)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setStatus(count % 2 == 0 ? Status.OPEN : Status.CLOSED);
                                }
                            })
                    .build()),
    LOCATION(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.LOCATION)
                    .sortableValueFunction(ApplicationList::getOtherLocation)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setOtherLocation(PrimitiveDataGenerator.generate(35));
                                }
                            })
                    .build()),
    ENTRIES_COUNT(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.ENTRY_COUNT)
                    .sortableValueFunction(
                            (keyable) -> {
                                return Integer.valueOf(keyable.getEntries().size());
                            })
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {

                                    for (int i = 0; i < count; i++) {
                                        ApplicationListEntry entry =
                                                new AppListEntryTestData().someComplete();
                                        ApplicationCode code =
                                                new ApplicationCodeTestData().someComplete();
                                        StandardApplicant saApplicant =
                                                new StandardApplicantTestData().someComplete();
                                        NameAddress applicant =
                                                new NameAddressTestData().someComplete();
                                        NameAddress respondent =
                                                new NameAddressTestData().someComplete();
                                        entry.setOfficials(List.of());
                                        entry.setResolutions(List.of());
                                        entry.setEntryFeeIds(new ArrayList<>());
                                        entry.setEntryFeeStatuses(new ArrayList<>());
                                        entry.setApplicationCode(code);
                                        entry.setStandardApplicant(saApplicant);
                                        entry.setAnamedaddress(applicant);
                                        entry.setRnameaddress(respondent);
                                        entry.setApplicationList(keyable);
                                    }
                                }
                            })
                    .build()),
    DESCRIPTION(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.DESCRIPTION)
                    .sortableValueFunction(ApplicationList::getDescription)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setDescription(PrimitiveDataGenerator.generate());
                                }
                            })
                    .build()),
    OTHER_LOCATION_DESCRIPTION(
            SortMetaDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.OTHER_LOCATION)
                    .sortableValueFunction(ApplicationList::getDescription)
                    .defaultSort(true)
                    .sortGenerator(
                            new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(
                                        int count,
                                        ApplicationList keyable,
                                        SortMetaDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setOtherLocation(PrimitiveDataGenerator.generate());
                                }
                            })
                    .build()),
    ;

    private SortMetaDataDescriptor<ApplicationList> sortDataDescriptor;

    ApplicationListSortEnum(SortMetaDataDescriptor<ApplicationList> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<ApplicationList> getDescriptor() {
        return sortDataDescriptor;
    }
}
