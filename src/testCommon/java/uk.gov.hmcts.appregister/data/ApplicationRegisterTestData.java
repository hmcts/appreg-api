package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;

public class ApplicationRegisterTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ApplicationRegister, ApplicationRegister.ApplicationRegisterBuilder> {
    @Override
    public ApplicationRegister.ApplicationRegisterBuilder someMinimal() {
        ApplicationRegister.ApplicationRegisterBuilder data = ApplicationRegister.builder();
        return data.applicationList(new AppListTestData().someMinimal().build());
    }

    @Override
    public ApplicationRegister someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        ApplicationRegister register =
                Instancio.of(ApplicationRegister.class)
                        .ignore(field(ApplicationRegister::getId))
                        .ignore(field(ApplicationRegister::getApplicationList))
                        .withSettings(settings)
                        .create();
        register.setApplicationList(new AppListTestData().someMinimal().build());
        return register;
    }
}
