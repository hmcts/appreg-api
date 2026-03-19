package uk.gov.hmcts.appregister.util;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import java.time.LocalDate;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
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
        return getCorrectCreateEntryDto(false);
    }

    /**
     * gets the correct payload to make a successful create entry.
     *
     * @param satisfyForClose Satisfy condition for app list closure
     * @return The created payload
     */
    public EntryCreateDto getCorrectCreateEntryDto(boolean satisfyForClose) {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        List<Official> officials = Instancio.ofList(Official.class).size(4).create();
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(officials);

        if (satisfyForClose) {
            FeeStatus feeStatus = new FeeStatus();

            // if we want to satisfy for close set to paid
            feeStatus.setPaymentStatus(PaymentStatus.PAID);
            feeStatus.setStatusDate(LocalDate.now());
            entryCreateDto.setFeeStatuses(List.of(feeStatus));
        }

        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getRespondent().setOrganisation(null);

        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AS12 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));

        ;

        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of(null));

        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AS12 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("RESPONDENT@TEST.COM"));

        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("09876543210"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));

        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("01234567890"));

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        // Ensure rule compliance
        sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        return entryCreateDto;
    }

    public static void sanitiseFeeStatusesForDueRule(List<FeeStatus> feeStatuses) {
        if (feeStatuses == null) {
            return;
        }

        for (FeeStatus fs : feeStatuses) {
            if (fs != null && fs.getPaymentStatus() == DUE) {
                fs.setPaymentReference(null);
            }
        }
    }
}
