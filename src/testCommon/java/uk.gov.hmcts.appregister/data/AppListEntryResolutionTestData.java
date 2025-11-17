package uk.gov.hmcts.appregister.data;

import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;

public class AppListEntryResolutionTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                AppListEntryResolution, AppListEntryResolution.AppListEntryResolutionBuilder> {

    public static final String WORDING_1 = "Wording 1";
    public static final String WORDING_2 = "Wording 2";

    @Override
    public AppListEntryResolution.AppListEntryResolutionBuilder someMinimal() {
        return AppListEntryResolution.builder().resolutionOfficer("Officer").version(1L);
    }
}
