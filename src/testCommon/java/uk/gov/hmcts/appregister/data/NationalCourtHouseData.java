package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
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
        long suffix = Math.abs(id.getMostSignificantBits() % 1000000);

        return NationalCourtHouse.builder()
                .id(Math.abs(id.getMostSignificantBits()))
                .courtLocationCode("NCH" + suffix)
                .name(StringUtil.stripToMax("name " + id, 100))
                .startDate(LocalDate.now(java.time.ZoneOffset.UTC))
                .courtType("CHOA");
    }

    @Override
    public NationalCourtHouse.NationalCourtHouseBuilder someMaximal() {
        return uk.gov.hmcts.appregister.testutils.data.Persistable.super.someMaximal();
    }

    @Override
    public NationalCourtHouse someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        UUID id = UUID.randomUUID();

        NationalCourtHouse court =
                Instancio.of(NationalCourtHouse.class)
                        .ignore(field(NationalCourtHouse::getId))
                        .ignore(field(NationalCourtHouse::getVersion))
                        .withSettings(settings)
                        .create();

        court.setId(Math.abs(id.getMostSignificantBits()));
        court.setCourtLocationCode("NCH" + Math.abs(id.getMostSignificantBits() % 1000000));
        return court;
    }
}
