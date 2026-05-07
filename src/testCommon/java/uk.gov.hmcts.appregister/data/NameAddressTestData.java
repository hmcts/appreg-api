package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.time.LocalDate;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;

public class NameAddressTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                NameAddress, NameAddress.NameAddressBuilder> {

    @Override
    public NameAddress.NameAddressBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        NameAddress.NameAddressBuilder data = NameAddress.builder();
        data.dateOfBirth(LocalDate.now())
                .mobileNumber("number" + uniqueId)
                .address1("address1" + uniqueId)
                .address2("address2" + uniqueId)
                .address3("address3" + uniqueId)
                .address4("address4" + uniqueId)
                .address5("address5" + uniqueId)
                .forename1("forename1" + uniqueId)
                .forename2("forename2" + uniqueId)
                .forename3("forename3" + uniqueId)
                .dmsId("dmsId" + uniqueId)
                .emailAddress("emailAddress" + uniqueId)
                .postcode("postcode" + uniqueId)
                .title("title" + uniqueId)
                .surname("surname" + uniqueId)
                .telephoneNumber("telephoneNumber" + uniqueId)
                .name("name" + uniqueId)
                .build();

        return data;
    }

    @Override
    public NameAddress someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        NameAddress address =
                Instancio.of(NameAddress.class)
                        .ignore(field(NameAddress::getId))
                        .ignore(field(NameAddress::getVersion))
                        .withSettings(settings)
                        .create();

        // ensure an applicant that is an organisation
        address.setCode(NameAddressCodeType.APPLICANT);
        address.setName(null);
        return address;
    }

    /**
     * generates a person.
     *
     * @return address representing a person
     */
    public NameAddress somePerson() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        return Instancio.of(NameAddress.class)
                .ignore(field(NameAddress::getId))
                .ignore(field(NameAddress::getVersion))
                .ignore(field(NameAddress::getName))
                .withSettings(settings)
                .create();
    }

    /**
     * generates an organisation.
     *
     * @return address representing an organisation
     */
    public NameAddress someOrganisation() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        return Instancio.of(NameAddress.class)
                .ignore(field(NameAddress::getId))
                .ignore(field(NameAddress::getVersion))
                .ignore(field(NameAddress::getTitle))
                .ignore(field(NameAddress::getSurname))
                .ignore(field(NameAddress::getForename1))
                .ignore(field(NameAddress::getForename2))
                .ignore(field(NameAddress::getForename3))
                .withSettings(settings)
                .create();
    }
}
