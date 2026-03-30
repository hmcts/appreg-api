package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

import java.time.LocalDate;

public class ResolutionCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ResolutionCode, ResolutionCode.ResolutionCodeBuilder> {

    @Override
    public ResolutionCode someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(ResolutionCode.class)
                .ignore(field(ResolutionCode::getId))
                .ignore(field(ResolutionCode::getVersion))
                .set(field(ResolutionCode::getStartDate), LocalDate.now().minusDays(10))
                .set(field(ResolutionCode::getEndDate), LocalDate.now().plusDays(10))
            .withSettings(settings)
                .create();
    }
}
