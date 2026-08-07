package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

public class AppListTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ApplicationList, ApplicationList.ApplicationListBuilder> {

    @Override
    public ApplicationList.ApplicationListBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        return ApplicationList.builder()
                .description("Description " + uniqueId)
                .date(LocalDate.now(java.time.ZoneOffset.UTC))
                .time(LocalTime.now(java.time.Clock.systemUTC()));
    }

    @Override
    public ApplicationList someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(ApplicationList.class)
                .ignore(field(ApplicationList::getId))
                .ignore(field(ApplicationList::getCja))
                .withSettings(settings)
                .create();
    }
}
