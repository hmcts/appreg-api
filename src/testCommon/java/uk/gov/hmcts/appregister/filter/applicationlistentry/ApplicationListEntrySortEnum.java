package uk.gov.hmcts.appregister.filter.applicationlistentry;

import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntryByListIdSortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.filter.FilterFieldData;
import uk.gov.hmcts.appregister.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.filter.meta.GenerateAccordingToSort;
import uk.gov.hmcts.appregister.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.filter.meta.SortMetaDescriptorEnum;

import java.util.ArrayList;

public enum ApplicationListEntrySortEnum implements SortMetaDescriptorEnum<ApplicationListEntry> {
    DATE(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
                    .sortableOperationEnum(ApplicationEntrySortFieldEnum.DATE)
            .sortableValueFunction((keyable) -> keyable.getApplicationList().getDate())
        .sortGenerator(
        new GenerateAccordingToSort<ApplicationListEntry>() {
        @Override
        public void apply(
        int count,
        ApplicationListEntry keyable,
        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {
            getList(keyable).setDate(PrimitiveDataGenerator.getDate(count));
        }
    }).build()),
    APPLICANT(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .sortableOperationEnum(
                ApplicationEntrySortFieldEnum.APPLICANT)
            .sortableValueFunction(keyable
                                       ->
                                       keyable.getAnamedaddress().getName() != null
                                           ? keyable.getAnamedaddress().getName()
                                           : keyable.getAnamedaddress().getForename1() + " " +
                                           keyable.getAnamedaddress().getSurname())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {

                        if (isOrganisation(count)) {
                            getApplicant(keyable, true);
                        } else {
                            getApplicant(keyable, false);
                        }
                    }}).build()),
    RESPONDENT(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .sortableOperationEnum(
                ApplicationEntrySortFieldEnum.RESPONDENT)
            .sortableValueFunction(keyable
                                       ->
                                       keyable.getRnameaddress().getName() != null
                                           ? keyable.getRnameaddress().getName()
                                           : keyable.getRnameaddress().getForename1() + " " +
                                           keyable.getAnamedaddress().getSurname())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {

                        if (isOrganisation(count)) {
                            getRespondent(keyable, true);
                        } else {
                            getRespondent(keyable, false);
                        }
                    }
                }).build()),
    APPLICATION_TITLE(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .defaultSort(true)
            .sortableOperationEnum(ApplicationEntrySortFieldEnum.APPLICATION_TITLE)
            .sortableValueFunction((keyable) -> keyable.getApplicationCode().getTitle())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {
                        ApplicationCode applicationCodeTestData = getCode(keyable);
                        applicationCodeTestData.setTitle(
                            (PrimitiveDataGenerator.generate(count, 10)));
                        applicationCodeTestData.setCode(
                            (PrimitiveDataGenerator.generate(count, 10)));
                        keyable.setApplicationCode(applicationCodeTestData);
                    }
                })
            .build()),
    RESULTED(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .sortableOperationEnum(ApplicationEntrySortFieldEnum.RESULTED)
            .sortableValueFunction(
                keyable ->
                    keyable.getResolutions()
                        .get(0)
                        .getResolutionCode()
                        .getResultCode())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {
                        ResolutionCode resolutionCode =
                            new ResolutionCodeTestData().someComplete();
                        resolutionCode.setResultCode(
                            PrimitiveDataGenerator.generate(count, 10));

                        AppListEntryResolution resolution =
                            new AppListEntryResolutionTestData().someComplete();
                        resolution.setResolutionCode(resolutionCode);
                        keyable.setResolutions(new ArrayList<>());
                        keyable.getResolutions().add(resolution);
                    }
                })
            .build()),
    FEE(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .sortableOperationEnum(ApplicationEntrySortFieldEnum.FEE_REQUIRED)
            .sortableValueFunction(
                keyable -> keyable.getApplicationCode().getFeeDue().isYes())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {
                        Boolean fee = count % 2 == 0;
                        getCode(keyable)
                            .setFeeDue(fee ? YesOrNo.YES : YesOrNo.NO);
                    }
                })
            .build()),
    STATUS(
        SortMetaDataDescriptor.<ApplicationListEntry>builder()
            .sortableOperationEnum(ApplicationEntrySortFieldEnum.STATUS)
            .sortableValueFunction(
                keyable -> keyable.getApplicationList().getStatus().getValue())
            .sortGenerator(
                new GenerateAccordingToSort<ApplicationListEntry>() {
                    @Override
                    public void apply(
                        int count,
                        ApplicationListEntry keyable,
                        SortMetaDataDescriptor<ApplicationListEntry> descriptor) {
                        getList(keyable).setStatus(count % 2 == 0 ? Status.OPEN : Status.CLOSED);
                    }
                })
            .build());


    private SortMetaDataDescriptor<ApplicationListEntry> sortDataDescriptor;

    ApplicationListEntrySortEnum(
        SortMetaDataDescriptor<ApplicationListEntry> sortDataDescriptor) {
        this.sortDataDescriptor = sortDataDescriptor;
    }

    @Override
    public SortMetaDataDescriptor<ApplicationListEntry> getDescriptor() {
        return sortDataDescriptor;
    }


    public static NameAddress getApplicant(ApplicationListEntry applicationListEntry, boolean isOrganisation) {
        if (applicationListEntry.getAnamedaddress() == null) {
            applicationListEntry.setAnamedaddress(isOrganisation ? new NameAddressTestData().someOrganisation() :
                                                      new NameAddressTestData().somePerson());
            applicationListEntry.getAnamedaddress().setCode(NameAddressCodeType.APPLICANT);
            return applicationListEntry.getAnamedaddress();
        }
        return applicationListEntry.getAnamedaddress();
    }

    public static NameAddress getRespondent(ApplicationListEntry applicationListEntry,  boolean isOrganisation) {
        if (applicationListEntry.getRnameaddress() == null) {
            applicationListEntry.setRnameaddress(isOrganisation ? new NameAddressTestData().someOrganisation() :
                                                      new NameAddressTestData().somePerson());
            applicationListEntry.getRnameaddress().setCode(NameAddressCodeType.RESPONDENT);
            return applicationListEntry.getRnameaddress();
        }
        return applicationListEntry.getRnameaddress();
    }

    public static ApplicationList getList(ApplicationListEntry applicationListEntry) {
        if (applicationListEntry.getApplicationList() == null) {
            ApplicationList applicationList = new AppListTestData().someComplete();
            applicationListEntry.setApplicationList(applicationList);
            return applicationListEntry.getApplicationList();
        }
        return applicationListEntry.getApplicationList();
    }

    public static ApplicationCode getCode(ApplicationListEntry applicationListEntry) {
        if (applicationListEntry.getApplicationList() == null) {
            ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
            applicationListEntry.setApplicationCode(applicationCode);
            return applicationListEntry.getApplicationCode();
        }
        return applicationListEntry.getApplicationCode();
    }

    public static boolean isOrganisation(int count) {
        return count % 2 == 1;
    }
}
