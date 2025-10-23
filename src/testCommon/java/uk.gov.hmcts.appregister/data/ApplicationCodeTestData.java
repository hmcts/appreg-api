package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.util.StringUtil;

public class ApplicationCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ApplicationCode, ApplicationCode.ApplicationCodeBuilder> {
    @Override
    public ApplicationCode.ApplicationCodeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationCode.ApplicationCodeBuilder data = ApplicationCode.builder();
        return data.code(StringUtil.stripToMax(uniqueId.toString(), 10))
                .title("title" + uniqueId)
                .wording("wording" + uniqueId)
                .feeDue(YesOrNo.YES)
                .requiresRespondent(YesOrNo.YES)
                .startDate(LocalDate.now(ZoneOffset.UTC))
                .bulkRespondentAllowed(YesOrNo.YES);
    }

    @Override
    public ApplicationCode someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(ApplicationCode.class)
                .ignore(field(ApplicationCode::getId))
                .ignore(field(ApplicationCode::getVersion))
                .ignore(field(ApplicationCode::getApplicationListEntryList))
                .withSettings(settings)
                .create();
    }
}
