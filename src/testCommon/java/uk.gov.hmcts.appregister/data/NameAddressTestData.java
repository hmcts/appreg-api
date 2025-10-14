package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

public class NameAddressTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                NameAddress, NameAddress.NameAddressBuilder> {

    @Override
    public NameAddress.NameAddressBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        NameAddress.NameAddressBuilder data = NameAddress.builder();
        data.dateOfBirth(OffsetDateTime.now())
                .mobileNumber("number" + uniqueId)
                .address1("address1" + uniqueId)
                .address2("address2" + uniqueId)
                .address3("address3" + uniqueId)
                .address4("address4" + uniqueId)
                .address5("address5" + uniqueId)
                .forename1("forename1" + uniqueId)
                .forename2("forename2" + uniqueId)
                .forename3("forename3" + uniqueId)
                .code("code" + uniqueId)
                .dmsId("dmsId" + uniqueId)
                .emailAddress("emailAddress" + uniqueId)
                .postcode("postcode" + uniqueId)
                .title("title" + uniqueId)
                .surname("surname" + uniqueId)
                .telephoneNumber("telephoneNumber" + uniqueId)
                .dateOfBirth(OffsetDateTime.now())
                .name("name" + uniqueId)
                .build();

        return data;
    }

    @Override
    public NameAddress someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        return Instancio.of(NameAddress.class)
                .ignore(field(NameAddress::getId))
                .ignore(field(NameAddress::getVersion))
                .withSettings(settings)
                .create();
    }
}
