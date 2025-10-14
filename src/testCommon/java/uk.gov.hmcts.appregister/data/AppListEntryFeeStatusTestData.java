package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

public class AppListEntryFeeStatusTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                AppListEntryFeeStatus, AppListEntryFeeStatus.AppListEntryFeeStatusBuilder> {
    @Override
    public AppListEntryFeeStatus someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        AppListEntryFeeStatus appListEntryFeeStatus =
                Instancio.of(AppListEntryFeeStatus.class)
                        .ignore(field(AppListEntryFeeStatus::getId))
                        .ignore(field(AppListEntryFeeStatus::getVersion))
                        .ignore(field(AppListEntryFeeStatus::getAlefsFeeStatus))
                        .ignore(field(AppListEntryFeeStatus::getAppListEntry))
                        .withSettings(settings)
                        .create();

        ApplicationListEntry applicationListEntry =
                new AppListEntryTestData().someMinimal().build();
        appListEntryFeeStatus.setAlefsFeeStatus(FeeStatusType.DUE.getDisplayName());
        appListEntryFeeStatus.setAppListEntry(applicationListEntry);

        return appListEntryFeeStatus;
    }
}
