package uk.gov.hmcts.appregister.testutils.util;

import static uk.gov.hmcts.appregister.common.util.OfficialTypeUtil.CLERK_CODE;
import static uk.gov.hmcts.appregister.common.util.OfficialTypeUtil.MAGISTRATE_CODE;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_1;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_2;
import static uk.gov.hmcts.appregister.util.TestConstants.MR;
import static uk.gov.hmcts.appregister.util.TestConstants.MRS;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_SURNAME;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON2_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON2_SURNAME;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON3_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON3_SURNAME;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.AppListEntryOfficialTestData;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;

public final class ApplicationListEntryUtil {

    public static ApplicationListEntry saveApplicationListEntry(
            EntityManager entityManager,
            DatabasePersistance persistance,
            ApplicationList list,
            Short sequenceNumber) {
        ResolutionCode resolutionCode = new ResolutionCodeTestData().someComplete();
        entityManager.persist(resolutionCode);
        entityManager.flush();

        ApplicationListEntry listEntryData =
                new AppListEntryTestData().createApplicationListEntry(list, sequenceNumber);

        listEntryData.setAccountNumber("1234567890");
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
        listEntryData.setStandardApplicant(standardApplicant);
        NameAddress nameAddress = new NameAddressTestData().someComplete();
        listEntryData.setRnameaddress(nameAddress);

        List<AppListEntryResolution> resolutions = new ArrayList<>();
        AppListEntryResolution appListEntryResolution1 =
                new AppListEntryResolutionTestData()
                        .someMinimal()
                        .resolutionWording(WORDING_1)
                        .applicationList(listEntryData)
                        .resolutionCode(resolutionCode)
                        .build();
        resolutions.add(appListEntryResolution1);
        AppListEntryResolution appListEntryResolution2 =
                new AppListEntryResolutionTestData()
                        .someMinimal()
                        .resolutionWording(WORDING_2)
                        .applicationList(listEntryData)
                        .resolutionCode(resolutionCode)
                        .build();
        resolutions.add(appListEntryResolution2);
        listEntryData.setResolutions(resolutions);

        List<AppListEntryOfficial> officials = new ArrayList<>();
        AppListEntryOfficial appListEntryOfficial1 =
                new AppListEntryOfficialTestData()
                        .someMinimal()
                        .appListEntry(listEntryData)
                        .title(MR)
                        .forename(PERSON1_FORENAME1)
                        .surname(PERSON1_SURNAME)
                        .officialType(MAGISTRATE_CODE)
                        .build();
        officials.add(appListEntryOfficial1);
        AppListEntryOfficial appListEntryOfficial2 =
                new AppListEntryOfficialTestData()
                        .someMinimal()
                        .appListEntry(listEntryData)
                        .title(MRS)
                        .forename(PERSON2_FORENAME1)
                        .surname(PERSON2_SURNAME)
                        .officialType(MAGISTRATE_CODE)
                        .build();
        officials.add(appListEntryOfficial2);
        AppListEntryOfficial appListEntryOfficial3 =
                new AppListEntryOfficialTestData()
                        .someMinimal()
                        .appListEntry(listEntryData)
                        .title(MR)
                        .forename(PERSON3_FORENAME1)
                        .surname(PERSON3_SURNAME)
                        .officialType(CLERK_CODE)
                        .build();
        officials.add(appListEntryOfficial3);
        listEntryData.setOfficials(officials);

        final ApplicationListEntry data = persistance.save(listEntryData);

        for (AppListEntryResolution resolution : resolutions) {
            entityManager.persist(resolution);
        }

        for (AppListEntryOfficial official : officials) {
            persistance.save(official);
        }

        entityManager.flush();

        return data;
    }
}
