package uk.gov.hmcts.appregister.testutils.data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

public class AppListEntryData
        implements Persistable<ApplicationListEntry.ApplicationListEntryBuilder> {

    @Override
    public ApplicationListEntry.ApplicationListEntryBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationList list = new AppListData().someMinimal().build();
        ApplicationCode code = new ApplicationCodeTestData().someMinimal().build();
        ApplicationListEntry.ApplicationListEntryBuilder applicationListEntryBuilder =
                ApplicationListEntry.builder()
                        .applicationCode(code)
                        .applicationList(list)
                        .applicationListEntryWording("Wording " + uniqueId)
                        .entryRescheduled("1")
                        .sequenceNumber(Short.MIN_VALUE)
                        .lodgementDate(OffsetDateTime.now(ZoneId.of("UTC")));
        return applicationListEntryBuilder;
    }
}
