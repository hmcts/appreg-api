package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.util.StringUtil;

public class ApplicationCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ApplicationCode, ApplicationCode.ApplicationCodeBuilder> {
    private static final AtomicLong NEXT_APPLICATION_CODE_ID = new AtomicLong(321365000L);

    @Override
    public ApplicationCode.ApplicationCodeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationCode.ApplicationCodeBuilder data = ApplicationCode.builder();
        return data.id(NEXT_APPLICATION_CODE_ID.getAndIncrement())
                .code(StringUtil.stripToMax(uniqueId.toString(), 10))
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
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        ApplicationCode applicationCode =
                Instancio.of(ApplicationCode.class)
                        .ignore(field(ApplicationCode::getId))
                        .ignore(field(ApplicationCode::getVersion))
                        .ignore(field(ApplicationCode::getApplicationListEntryList))
                        .set(field(ApplicationCode::getStartDate), today.minusDays(10))
                        .set(field(ApplicationCode::getEndDate), today.plusDays(10))
                        .withSettings(settings)
                        .create();
        applicationCode.setId(NEXT_APPLICATION_CODE_ID.getAndIncrement());
        return applicationCode;
    }
}
