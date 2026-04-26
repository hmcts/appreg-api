package uk.gov.hmcts.appregister.filter.applicationlistentry;

import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.filter.FilterFieldData;
import uk.gov.hmcts.appregister.filter.FilterFieldValue;
import uk.gov.hmcts.appregister.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.filter.meta.FilterMetaDescriptorEnum;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * An enumeration that allows us to setup filter for the application list entry endpoint.
 */
public enum ApplicationListEntryFilterEnum
        implements FilterMetaDescriptorEnum<ApplicationListEntry> {
    DATE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("date")
            .partialSupport(false)
            .caseInsensitive(false)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        new FilterFieldData<>();
                    filterFieldData.setDescriptor(descriptor);

                    FilterFieldValue<ApplicationListEntry> filterFieldValue =
                        new FilterFieldValue<>();
                    filterFieldValue.setKeyable(keyable);
                    filterFieldValue.setValue(PrimitiveDataGenerator.getDate(count));
                    filterFieldValue.setKeyable(keyable);
                    getList(keyable).setDate((LocalDate) filterFieldValue.getValue());

                    filterFieldData.setKeyableValues(filterFieldValue);
                    return filterFieldData;
                })
            .build()),
    APPLICANT(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("applicantName")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 10);

                    if (isOrganisation(count)) {
                        filterFieldData.getKeyableValues().setValue(
                            getName(getApplicant(keyable, true),
                                    filterFieldData.getKeyableValues().getValue().toString()));
                    } else {

                        FilterFieldData<ApplicationListEntry> surName =
                            FilterFieldDataGenerator.getFieldDataWithString(
                                count, descriptor, keyable, 10);

                        filterFieldData.getKeyableValues().setValue(
                            getForename(getApplicant(keyable, false),
                                    filterFieldData.getKeyableValues().getValue().toString()));
                        getSurname(getApplicant(keyable, false),
                               surName.getKeyableValues().getValue().toString());
                    }

                    return filterFieldData;
                })
            .build()),
    RESPONDENT(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("respondentName")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 10);

                    if (isOrganisation(count)) {
                        filterFieldData.getKeyableValues().setValue(getName(
                            getRespondent(keyable, true),
                            filterFieldData.getKeyableValues().getValue().toString()));
                    } else {
                        FilterFieldData<ApplicationListEntry> surName =
                            FilterFieldDataGenerator.getFieldDataWithString(
                                count, descriptor, keyable, 10);
                        filterFieldData.getKeyableValues().setValue(getForename(
                            getRespondent(keyable, false),
                                filterFieldData.getKeyableValues().toString()));
                        getSurname(getRespondent(keyable, false),
                                                 surName.getKeyableValues()
                                                     .getValue().toString());
                    }

                    return filterFieldData;
                })
            .build()),

    COURT_CODE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("courtCode")
            .partialSupport(false)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    ApplicationList applicationList = getList(keyable);
                    applicationList.setCourtCode(filterFieldData.getKeyableValues().getValue().toString());

                    return filterFieldData;
                })
            .build()),
    OTHER_LOCATION_DESCRIPTION(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("otherLocationDescription")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    getList(keyable).setOtherLocation(filterFieldData.getKeyableValues().getValue().toString());

                    return filterFieldData;
                })
            .build()),

    CJA_CODE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("cjaCode")
            .partialSupport(false)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 2);

                    CriminalJusticeTestData criminalJusticeTestData = new CriminalJusticeTestData();
                    CriminalJusticeArea criminalJusticeArea = criminalJusticeTestData.someComplete();
                    getList(keyable).setCja(criminalJusticeArea);
                    criminalJusticeArea.setCode(filterFieldData.getKeyableValues().getValue().toString());
                    return filterFieldData;
                })
            .build()),
    APPLICANT_ORGANISATION(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("applicantOrganisation")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    if (isOrganisation(count)) {
                        filterFieldData.getKeyableValues().setValue(getName(getApplicant(keyable, true),
                                filterFieldData.getKeyableValues().getValue().toString()));
                    } else {
                        filterFieldData.setKeyableValues(new FilterFieldValue<>(keyable, null));
                    }

                    return filterFieldData;
                })
            .build()),
    APPLICANT_SURNAME(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("applicantSurname")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    if (!isOrganisation(count)) {
                        filterFieldData.getKeyableValues().setValue(getSurname(getRespondent(keyable, false),
                                   filterFieldData.getKeyableValues().toString()));
                    } else {
                        filterFieldData.setKeyableValues(new FilterFieldValue<>(keyable, null));
                    }

                    return filterFieldData;
                })
            .build()),
    STANDARD_APPLICANT_CODE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("standardApplicantCode")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);
                    StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
                    keyable.setStandardApplicant(standardApplicant);
                    standardApplicant.setApplicantCode(filterFieldData.getKeyableValues().getValue().toString());
                    return filterFieldData;
                })
            .build()),
    STATUS(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("status")
            .partialSupport(false)
            .caseInsensitive(false)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        new FilterFieldData<>();
                    filterFieldData.setDescriptor(descriptor);
                    FilterFieldValue<ApplicationListEntry> filterFieldValue =
                        new FilterFieldValue<>();
                    filterFieldValue.setKeyable(keyable);
                    filterFieldData.setKeyableValues(filterFieldValue);
                    keyable.getApplicationList().setStatus(count % 2 == 0 ? Status.OPEN : Status.CLOSED);
                    filterFieldValue.setValue(keyable.getApplicationList().getStatus());
                    return filterFieldData;
                })
            .build()),
    RESPONDENT_ORGANISATION(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("respondentOrganisation")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    if (isOrganisation(count)) {
                        filterFieldData.getKeyableValues().setValue(
                            getName(getRespondent(keyable, true),
                                filterFieldData.getKeyableValues().getValue().toString()));
                    } else {
                        filterFieldData.setKeyableValues(new FilterFieldValue<>(keyable, null));
                    }

                    return filterFieldData;
                })
            .build()),
    RESPONDENT_SURNAME(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("respondentSurname")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    if (!isOrganisation(count)) {
                        filterFieldData =
                            FilterFieldDataGenerator.getFieldDataWithString(
                                count, descriptor, keyable, 8);
                        filterFieldData.getKeyableValues().setValue(
                            getSurname(getRespondent(keyable, false), filterFieldData
                            .getKeyableValues().getValue().toString()));
                    } else {
                        filterFieldData.setKeyableValues(new FilterFieldValue<>(keyable, null));
                    }

                    return filterFieldData;
                })
            .build()),
    RESPONDENT_POSTCODE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("respondentPostcode")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    keyable.getRnameaddress().setPostcode(filterFieldData.getKeyableValues().getValue().toString());

                    return filterFieldData;
                })
            .build()),
    ACCOUNT_REFERENCE(
        FilterFieldDataMetaDescriptor.<ApplicationListEntry>builder()
            .queryName("accountReference")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(
                (count, keyable, descriptor) -> {
                    FilterFieldData<ApplicationListEntry> filterFieldData =
                        FilterFieldDataGenerator.getFieldDataWithString(
                            count, descriptor, keyable, 8);

                    keyable.setAccountNumber(filterFieldData.getKeyableValues().getValue().toString());

                    return filterFieldData;
                })
            .build());

    private FilterFieldDataMetaDescriptor filterFieldDataDescriptor;

    ApplicationListEntryFilterEnum(FilterFieldDataMetaDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }

    public static NameAddress getApplicant(ApplicationListEntry applicationListEntry, boolean isOrganisation) {
        if (applicationListEntry.getAnamedaddress() == null) {
            applicationListEntry.setAnamedaddress(isOrganisation ? new NameAddressTestData().someOrganisation() :
                                                      new NameAddressTestData().somePerson());

            applicationListEntry.getAnamedaddress().setSurname(null);
            applicationListEntry.getAnamedaddress().setForename1(null);
            applicationListEntry.getAnamedaddress().setName(null);

            applicationListEntry.getAnamedaddress().setCode(NameAddressCodeType.APPLICANT);

            return applicationListEntry.getAnamedaddress();
        }
        return applicationListEntry.getAnamedaddress();
    }

    public static NameAddress getRespondent(ApplicationListEntry applicationListEntry,
                                            boolean isOrganisation) {
        if (applicationListEntry.getRnameaddress() == null) {
            applicationListEntry.setRnameaddress(isOrganisation ? new NameAddressTestData().someOrganisation() :
                                                     new NameAddressTestData().somePerson());
            applicationListEntry.getRnameaddress().setSurname(null);
            applicationListEntry.getRnameaddress().setForename1(null);
            applicationListEntry.getRnameaddress().setName(null);

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

    public static String getSurname(NameAddress address, String surname) {
        if (address.getSurname() == null) {
            address.setSurname(surname);
        }
        return address.getSurname();
    }

    public static String getForename(NameAddress address, String forename) {
        if (address.getForename1() == null) {
            address.setForename1(forename);
        }
        return address.getForename1();
    }


    public static String getName(NameAddress address, String name) {
        if (address.getName() == null) {
            address.setName(name);
        }
        return address.getName();
    }

    public static boolean isOrganisation(int count) {
        return count % 2 == 1;
    }
}
