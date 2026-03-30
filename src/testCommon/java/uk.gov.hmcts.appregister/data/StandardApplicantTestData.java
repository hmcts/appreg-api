package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

import java.time.LocalDate;

public class StandardApplicantTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                StandardApplicant, StandardApplicant.StandardApplicantBuilder> {

    @Override
    public StandardApplicant someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(StandardApplicant.class)
                .ignore(field(StandardApplicant::getId))
                .ignore(field(StandardApplicant::getVersion))
                .generate(field(StandardApplicant::getPostcode), gen -> gen.string().length(1, 8))
                .set(field(StandardApplicant::getApplicantStartDate), LocalDate.now().minusDays(10))
                .set(field(StandardApplicant::getApplicantEndDate), LocalDate.now().plusDays(10))
                .withSettings(settings)
                .create();
    }
}
