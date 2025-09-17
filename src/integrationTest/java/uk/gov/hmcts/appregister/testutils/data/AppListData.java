package uk.gov.hmcts.appregister.testutils.data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

public class AppListData implements Persistable<ApplicationList.ApplicationListBuilder> {
    @Override
    public ApplicationList.ApplicationListBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationList.ApplicationListBuilder applicationListBuilder =
                ApplicationList.builder()
                        .date(OffsetDateTime.now(ZoneId.of("UTC")))
                        .time(OffsetDateTime.now(ZoneId.of("UTC")))
                        .listDescription("Description" + uniqueId);
        return applicationListBuilder;
    }
}
