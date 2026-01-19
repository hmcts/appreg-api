package uk.gov.hmcts.appregister.util;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * Builds and prepares CreateEntryDto, applying required defaults.
 */
@UtilityClass
public class CreateEntryDtoUtil {

    /**
     * gets the correct payload to make a successful create entry.
     *
     * @return The created payload
     */
    public EntryCreateDto getCorrectCreateEntryDto() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        List<Official> officials = Instancio.ofList(Official.class).size(4).create();
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(officials);

        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail("RESPONDENT@TEST.COM");

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));
        return entryCreateDto;
    }
}
