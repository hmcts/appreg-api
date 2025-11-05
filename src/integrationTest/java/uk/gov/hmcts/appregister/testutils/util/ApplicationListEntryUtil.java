package uk.gov.hmcts.appregister.testutils.util;

import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_1;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_2;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
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

        ApplicationListEntry data = persistance.save(listEntryData);

        for (AppListEntryResolution resolution : resolutions) {
            entityManager.persist(resolution);
        }
        entityManager.flush();
        return data;
    }
}
