package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDateTime;
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
                .date(LocalDateTime.now())
                .time(LocalDateTime.now())
                .cja(new CriminalJusticeTestData().someMinimal().build());
    }

    @Override
    public ApplicationList someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationList list =
                Instancio.of(ApplicationList.class)
                        .ignore(field(ApplicationList::getPk))
                        .ignore(field(ApplicationList::getCja))
                        .withSettings(settings)
                        .create();

        list.setCja(new CriminalJusticeTestData().someMinimal().build());

        return list;
    }
}
