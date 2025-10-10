package uk.gov.hmcts.appregister.testutils.data;

import static org.instancio.Select.field;
import static uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper.TRUE_VALUE;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.testutils.StringUtil;

public class ApplicationCodeTestData
        implements Persistable<ApplicationCode.ApplicationCodeBuilder> {
    @Override
    public ApplicationCode.ApplicationCodeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationCode.ApplicationCodeBuilder data = ApplicationCode.builder();
        return data.code(StringUtil.stripToMax(uniqueId.toString(), 10))
                .title("title" + uniqueId)
                .wording("wording" + uniqueId)
                .feeDue("1")
                .requiresRespondent(TRUE_VALUE)
                .startDate(OffsetDateTime.now(ZoneOffset.UTC))
                .bulkRespondentAllowed(TRUE_VALUE);
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

    @Override
    public ApplicationCode.ApplicationCodeBuilder someMaximal() {
        return Persistable.super.someMaximal();
    }
}
