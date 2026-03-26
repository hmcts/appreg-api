package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.util.StringUtil;

public class NationalCourtHouseData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                NationalCourtHouse, NationalCourtHouse.NationalCourtHouseBuilder> {

    @Override
    public NationalCourtHouse.NationalCourtHouseBuilder someMinimal() {
        UUID id = UUID.randomUUID();
        var data = NationalCourtHouse.builder();
        data.courtLocationCode(StringUtil.stripToMax(id.toString(), 10))
                .name(StringUtil.stripToMax("name " + id, 100))
                .startDate(LocalDate.now())
                .changedBy(-125L)
                .changedDate(OffsetDateTime.now(ZoneId.of("UTC")))
                .courtType("CHOA");

        return data;
    }

    @Override
    public NationalCourtHouse.NationalCourtHouseBuilder someMaximal() {
        return uk.gov.hmcts.appregister.testutils.data.Persistable.super.someMaximal();
    }

    @Override
    public NationalCourtHouse someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        return Instancio.of(NationalCourtHouse.class)
                .ignore(field(NationalCourtHouse::getId))
                .ignore(field(NationalCourtHouse::getVersion))
                .withSettings(settings)
                .create();
    }
}
