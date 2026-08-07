package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.instancio.Instancio;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.util.StringUtil;

public class FeeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<Fee, Fee.FeeBuilder> {
    @Override
    public Fee.FeeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        Fee.FeeBuilder data = Fee.builder();
        data.reference(StringUtil.stripToMax(uniqueId.toString(), 12))
                .description("description" + uniqueId)
                .startDate(LocalDate.now(java.time.ZoneOffset.UTC))
                .amount(BigDecimal.valueOf(20))
                .build();

        return data;
    }

    @Override
    public Fee someComplete() {
        Fee fee =
                Instancio.of(Fee.class)
                        .ignore(field(Fee::getId))
                        .ignore(field(Fee::getVersion))
                        .create();

        fee.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        return fee;
    }
}
