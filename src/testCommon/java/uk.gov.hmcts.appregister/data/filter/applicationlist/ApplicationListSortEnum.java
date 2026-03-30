package uk.gov.hmcts.appregister.data.filter.applicationlist;

import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.criminaljusticearea.api.CriminalJusticeSortFieldEnum;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum ApplicationListSortEnum implements SortDescriptorEnum<ApplicationList> {


    DATE(SortDataDescriptor.<ApplicationList>builder()
             .sortableOperationEnum(ApplicationListSortFieldEnum.DATE)
             .sortableValueFunction(ApplicationList::getDate)
             .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                 @Override
                 public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                     keyable.setDate(PrimitiveDataGenerator.getDate(count));
                 }
             }).build()),
    TIME(SortDataDescriptor.<ApplicationList>builder()
             .sortableOperationEnum(ApplicationListSortFieldEnum.TIME)
             .sortableValueFunction(ApplicationList::getTime)
             .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                @Override
                                public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                                    keyable.setTime(PrimitiveDataGenerator.getTime(count));
                                }
                            }
             ).build()),
    STATUS(SortDataDescriptor.<ApplicationList>builder()
               .sortableOperationEnum(ApplicationListSortFieldEnum.STATUS)
               .sortableValueFunction(ApplicationList::getStatus)
               .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                  @Override
                                  public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                                      keyable.setStatus(count % 2 == 0 ? Status.OPEN : Status.CLOSED);
                                  }
                              }
               ).build()),
    LOCATION(SortDataDescriptor.<ApplicationList>builder()
                 .sortableOperationEnum(ApplicationListSortFieldEnum.LOCATION)
                 .sortableValueFunction(ApplicationList::getOtherLocation)
                 .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                    @Override
                                    public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                                        keyable.setOtherLocation(PrimitiveDataGenerator.generate(35));
                                    }
                                }
                 ).build()),
    ENTRIES_COUNT(SortDataDescriptor.<ApplicationList>builder()
                      .sortableOperationEnum(ApplicationListSortFieldEnum.ENTRY_COUNT)
                      .sortableValueFunction((keyable) -> {
                          return Integer.valueOf(keyable.getEntries().size());
                      })
                      .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                         @Override
                                         public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {

                                             for (int i=0; i < count; i++) {
                                                 ApplicationListEntry entry = new AppListEntryTestData().someComplete();
                                                 ApplicationCode code = new ApplicationCodeTestData().someComplete();
                                                 StandardApplicant saApplicant = new StandardApplicantTestData().someComplete();
                                                 NameAddress applicant = new NameAddressTestData().someComplete();
                                                 NameAddress respondent = new NameAddressTestData().someComplete();
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
                                     }
                      ).build()),
    DESCRIPTION(SortDataDescriptor.<ApplicationList>builder()
                    .sortableOperationEnum(ApplicationListSortFieldEnum.STATUS)
                    .sortableValueFunction(ApplicationList::getDescription).defaultSort(true)
                    .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                       @Override
                                       public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                                           keyable.setDescription(PrimitiveDataGenerator.generate());
                                       }
                                   }
                    ).build()),
    OTHER_LOCATION_DESCRIPTION(SortDataDescriptor.<ApplicationList>builder()
                                   .sortableOperationEnum(ApplicationListSortFieldEnum.OTHER_LOCATION)
                                   .sortableValueFunction(ApplicationList::getDescription).defaultSort(true)
                                   .sortGenerator(new GenerateAccordingToSort<ApplicationList>() {
                                          @Override
                                          public void apply(int count, ApplicationList keyable, SortDataDescriptor<ApplicationList> descriptor) {
                                              keyable.setOtherLocation(PrimitiveDataGenerator.generate());
                                          }
                                      }
                                   ).build()),
    ;

    private SortDataDescriptor<ApplicationList> sortDataDescriptor;

    ApplicationListSortEnum(SortDataDescriptor<ApplicationList> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;

    }

    @Override
    public SortDataDescriptor<ApplicationList> getDescriptor() {
        return sortDataDescriptor;
    }
}
