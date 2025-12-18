package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;

public class AppListEntryFeeIdTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                AppListEntryFeeId, AppListEntryFeeId.AppListEntryFeeIdBuilder> {
    @Override
    public AppListEntryFeeId someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        AppListEntryFeeId entryFeeId =
                Instancio.of(AppListEntryFeeId.class)
                        .ignore(field(AppListEntryFeeId::getFeeId))
                        .ignore(field(AppListEntryFeeId::getVersion))
                        .withSettings(settings)
                        .create();

        entryFeeId.setFeeId(new FeeTestData().someComplete().getId());
        entryFeeId.setAppListEntryId(new AppListEntryTestData().someComplete().getId());

        return entryFeeId;
    }
}
