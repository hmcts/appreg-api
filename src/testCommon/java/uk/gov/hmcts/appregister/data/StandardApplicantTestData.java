package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.concurrent.atomic.AtomicLong;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

public class StandardApplicantTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                StandardApplicant, StandardApplicant.StandardApplicantBuilder> {
    private static final AtomicLong NEXT_STANDARD_APPLICANT_ID = new AtomicLong(321365000L);

    @Override
    public StandardApplicant someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        StandardApplicant standardApplicant =
                Instancio.of(StandardApplicant.class)
                        .ignore(field(StandardApplicant::getId))
                        .ignore(field(StandardApplicant::getVersion))
                        .generate(
                                field(StandardApplicant::getPostcode),
                                gen -> gen.string().length(1, 8))
                        .withSettings(settings)
                        .create();
        standardApplicant.setId(NEXT_STANDARD_APPLICANT_ID.getAndIncrement());
        return standardApplicant;
    }
}
