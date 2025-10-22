package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

public class ResolutionCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
    ResolutionCode, ResolutionCode.ResolutionCodeBuilder> {

    @Override
    public ResolutionCode someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(ResolutionCode.class)
            .ignore(field(ResolutionCode::getId))
            .ignore(field(ResolutionCode::getVersion))
            .withSettings(settings)
            .create();
    }
}
