package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.util.StringUtil;

public class NationalCourtHouseData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                NationalCourtHouse, NationalCourtHouse.NationalCourtHouseBuilder> {
    private static final AtomicLong NEXT_NATIONAL_COURT_HOUSE_ID = new AtomicLong(321365000L);

    @Override
    public NationalCourtHouse.NationalCourtHouseBuilder someMinimal() {
        UUID id = UUID.randomUUID();
        var data = NationalCourtHouse.builder();
        data.id(NEXT_NATIONAL_COURT_HOUSE_ID.getAndIncrement())
                .courtLocationCode(StringUtil.stripToMax(id.toString(), 10))
                .name(StringUtil.stripToMax("name " + id, 100))
                .startDate(LocalDate.now(java.time.ZoneOffset.UTC))
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
        NationalCourtHouse nationalCourtHouse =
                Instancio.of(NationalCourtHouse.class)
                        .ignore(field(NationalCourtHouse::getId))
                        .ignore(field(NationalCourtHouse::getVersion))
                        .withSettings(settings)
                        .create();
        nationalCourtHouse.setId(NEXT_NATIONAL_COURT_HOUSE_ID.getAndIncrement());
        return nationalCourtHouse;
    }
}
