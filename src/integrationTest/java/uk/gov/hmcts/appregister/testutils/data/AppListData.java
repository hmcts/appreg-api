package uk.gov.hmcts.appregister.testutils.data;

import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

public class AppListData
        implements Persistable<ApplicationList, ApplicationList.ApplicationListBuilder> {
    @Override
    public ApplicationList.ApplicationListBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        return ApplicationList.builder().description("Description " + uniqueId);
    }
}
