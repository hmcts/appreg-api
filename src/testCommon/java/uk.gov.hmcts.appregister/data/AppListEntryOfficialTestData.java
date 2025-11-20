package uk.gov.hmcts.appregister.data;

import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;

public class AppListEntryOfficialTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                AppListEntryOfficial, AppListEntryOfficial.AppListEntryOfficialBuilder> {

    @Override
    public AppListEntryOfficial.AppListEntryOfficialBuilder someMinimal() {
        return AppListEntryOfficial.builder();
    }
}
