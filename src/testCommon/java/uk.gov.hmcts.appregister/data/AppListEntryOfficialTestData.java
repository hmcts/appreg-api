package uk.gov.hmcts.appregister.data;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.testutils.data.Persistable;
import static org.instancio.Select.field;

public class AppListEntryOfficialTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                AppListEntryOfficial, AppListEntryOfficial.AppListEntryOfficialBuilder> {

    @Override
    public AppListEntryOfficial.AppListEntryOfficialBuilder someMinimal() {
        return AppListEntryOfficial.builder();
    }

    @Override
    public AppListEntryOfficial someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(AppListEntryOfficial.class)
                .ignore(field(AppListEntryOfficial::getId))
                .ignore(field(AppListEntryOfficial::getAppListEntry))
                .withSettings(settings)
                .create();
   }
}
