package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.util.StringUtil;

public class FeeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<Fee, Fee.FeeBuilder> {
    private static final AtomicLong NEXT_FEE_ID = new AtomicLong(321365000L);

    @Override
    public Fee.FeeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        Fee.FeeBuilder data = Fee.builder();
        data.id(NEXT_FEE_ID.getAndIncrement())
                .reference(StringUtil.stripToMax(uniqueId.toString(), 12))
                .description("description" + uniqueId)
                .startDate(LocalDate.now(java.time.ZoneOffset.UTC))
                .amount(BigDecimal.valueOf(20))
                .build();

        return data;
    }

    @Override
    public Fee someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        Fee fee =
                Instancio.of(Fee.class)
                        .ignore(field(Fee::getId))
                        .ignore(field(Fee::getVersion))
                        .withSettings(settings)
                        .create();
        fee.setId(NEXT_FEE_ID.getAndIncrement());
        return fee;
    }
}
