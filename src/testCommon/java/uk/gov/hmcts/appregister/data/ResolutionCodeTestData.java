package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.concurrent.atomic.AtomicLong;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

public class ResolutionCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ResolutionCode, ResolutionCode.ResolutionCodeBuilder> {
    private static final AtomicLong NEXT_RESOLUTION_CODE_ID = new AtomicLong(321365000L);

    @Override
    public ResolutionCode someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ResolutionCode resolutionCode =
                Instancio.of(ResolutionCode.class)
                        .ignore(field(ResolutionCode::getId))
                        .ignore(field(ResolutionCode::getVersion))
                        .withSettings(settings)
                        .create();
        resolutionCode.setId(NEXT_RESOLUTION_CODE_ID.getAndIncrement());
        return resolutionCode;
    }
}
