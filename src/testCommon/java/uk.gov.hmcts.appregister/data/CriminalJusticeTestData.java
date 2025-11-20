package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.util.StringUtil;

public class CriminalJusticeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                CriminalJusticeArea, CriminalJusticeArea.CriminalJusticeAreaBuilder> {
    @Override
    public CriminalJusticeArea.CriminalJusticeAreaBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        CriminalJusticeArea.CriminalJusticeAreaBuilder data = CriminalJusticeArea.builder();
        data.code(StringUtil.stripToMax(uniqueId.toString(), 2))
                .description(StringUtil.stripToMax("description" + uniqueId, 35))
                .build();

        return data;
    }

    @Override
    public CriminalJusticeArea.CriminalJusticeAreaBuilder someMaximal() {
        return uk.gov.hmcts.appregister.testutils.data.Persistable.super.someMaximal();
    }

    @Override
    public CriminalJusticeArea someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(CriminalJusticeArea.class)
                .ignore(field(CriminalJusticeArea::getId))
                .withSettings(settings)
                .create();
    }
}
