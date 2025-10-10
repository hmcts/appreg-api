package uk.gov.hmcts.appregister.testutils.data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

public class AppListEntryData
        implements Persistable<ApplicationListEntry.ApplicationListEntryBuilder> {

    @Override
    public ApplicationListEntry.ApplicationListEntryBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
<<<<<<< Updated upstream:src/integrationTest/java/uk/gov/hmcts/appregister/testutils/data/AppListEntryData.java
        ApplicationList list = new AppListData().someMinimal().build();
        ApplicationCode code = new ApplicationCodeTestData().someMinimal().build();
=======
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationCode code = new ApplicationCodeTestData().someComplete();
>>>>>>> Stashed changes:src/testCommon/java/uk.gov.hmcts.appregister/data/AppListEntryTestData.java
        return ApplicationListEntry.builder()
                .applicationCode(code)
                .applicationList(list)
                .applicationListEntryWording("Wording " + uniqueId)
                .entryRescheduled(ApplicationListEntryMapper.TRUE_VALUE)
                .sequenceNumber(Short.MIN_VALUE)
                .lodgementDate(OffsetDateTime.now(ZoneId.of("UTC")));
    }
}
