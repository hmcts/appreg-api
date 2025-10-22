package uk.gov.hmcts.appregister.data;

import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;

public class AppListEntryResolutionTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
    AppListEntryResolution, AppListEntryResolution.AppListEntryResolutionBuilder> {

    @Override
    public AppListEntryResolution.AppListEntryResolutionBuilder someMinimal() {
        return AppListEntryResolution.builder()
            .resolutionWording("Wording")
            .resolutionOfficer("Officer")
            .version(1L);
    }
}
