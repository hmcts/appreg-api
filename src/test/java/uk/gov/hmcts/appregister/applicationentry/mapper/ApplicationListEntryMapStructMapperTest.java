package uk.gov.hmcts.appregister.applicationentry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.appregister.util.ApplicationListEntryPrintProjectionUtil.applicationListEntryPrintProjection;
import static uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionUtil.applicationListEntrySummaryProjection;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONCODE1_CODE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONCODE1_TITLE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_ACCOUNTNUMBER;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_CASEREFERENCE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_NOTES;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_WORDING;
import static uk.gov.hmcts.appregister.util.TestConstants.MR;
import static uk.gov.hmcts.appregister.util.TestConstants.MRS;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_NAME;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION1_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_NAME;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.ORGANISATION2_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_SURNAME;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_DATE_OF_BIRTH;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_SURNAME;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;

class ApplicationListEntryMapStructMapperTest {

    @Test
    void testToSummaryModel_provideValidData_validModelGenerated() {
        var uuid = UUID.randomUUID();
        var sequenceNumber = 1;
        var accountNumber = "1234567890";
        var applicant = "Mustafa's Org";
        var respondent = "Ahmed, Mustafa, His Majesty";
        var postCode = "SW1A 1AA";
        var applicationTitle = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired = true;
        var result = "APPC";
        var projection =
                applicationListEntrySummaryProjection()
                        .uuid(uuid)
                        .sequenceNumber(sequenceNumber)
                        .accountNumber(accountNumber)
                        .applicant(applicant)
                        .respondent(respondent)
                        .postCode(postCode)
                        .applicationTitle(applicationTitle)
                        .feeRequired(feeRequired)
                        .result(result)
                        .build();

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        var model = mapper.toSummaryDto(projection);

        assertApplicationListEntrySummary(
                uuid,
                sequenceNumber,
                model,
                accountNumber,
                applicant,
                respondent,
                postCode,
                applicationTitle,
                feeRequired,
                result);
    }

    @Test
    void testToSummaryModelList_provideValidData_validModelListGenerated() {
        var uuid1 = UUID.randomUUID();
        var sequenceNumber1 = 1;
        var accountNumber1 = "1234567890";
        var applicant1 = "Mustafa's Org";
        var respondent1 = "Ahmed, Mustafa, His Majesty";
        var postCode1 = "SW1A 1AA";
        var applicationTitle1 = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired1 = true;
        var result1 = "APPC";
        var projection1 =
                applicationListEntrySummaryProjection()
                        .uuid(uuid1)
                        .sequenceNumber(sequenceNumber1)
                        .accountNumber(accountNumber1)
                        .applicant(applicant1)
                        .respondent(respondent1)
                        .postCode(postCode1)
                        .applicationTitle(applicationTitle1)
                        .feeRequired(feeRequired1)
                        .result(result1)
                        .build();

        var uuid2 = UUID.randomUUID();
        var sequenceNumber2 = 2;
        var accountNumber2 = "1234567891";
        var applicant2 = "AW62958 300919";
        var respondent2 = "Johnson, Sarah";
        var postCode2 = "EH1 3QR";
        var applicationTitle2 = "Copy documents";
        var feeRequired2 = false;
        var result2 = "RESP";
        var projection2 =
                applicationListEntrySummaryProjection()
                        .uuid(uuid2)
                        .sequenceNumber(sequenceNumber2)
                        .accountNumber(accountNumber2)
                        .applicant(applicant2)
                        .respondent(respondent2)
                        .postCode(postCode2)
                        .applicationTitle(applicationTitle2)
                        .feeRequired(feeRequired2)
                        .result(result2)
                        .build();

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        List<ApplicationListEntrySummary> list =
                mapper.toSummaryDtoList(List.of(projection1, projection2));

        assertThat(list).hasSize(2);

        assertApplicationListEntrySummary(
                uuid1,
                sequenceNumber1,
                list.getFirst(),
                accountNumber1,
                applicant1,
                respondent1,
                postCode1,
                applicationTitle1,
                feeRequired1,
                result1);

        assertApplicationListEntrySummary(
                uuid2,
                sequenceNumber2,
                list.getLast(),
                accountNumber2,
                applicant2,
                respondent2,
                postCode2,
                applicationTitle2,
                feeRequired2,
                result2);
    }

    @Test
    void testToPrintDto_providePeople_validDtoGenerated() {
        var projection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicantTitle(MR)
                        .applicantSurname(PERSON4_SURNAME)
                        .applicantForename1(PERSON4_FORENAME1)
                        .applicantForename2(PERSON4_FORENAME2)
                        .applicantForename3(PERSON4_FORENAME3)
                        .applicantAddressLine1(PERSON4_ADDRESSLINE1)
                        .applicantAddressLine2(PERSON4_ADDRESSLINE2)
                        .applicantAddressLine3(PERSON4_ADDRESSLINE3)
                        .applicantAddressLine4(PERSON4_ADDRESSLINE4)
                        .applicantAddressLine5(PERSON4_ADDRESSLINE5)
                        .applicantPostcode(PERSON4_POSTCODE)
                        .applicantPhone(PERSON4_PHONE)
                        .applicantMobile(PERSON4_MOBILE)
                        .applicantEmail(PERSON4_EMAIL)
                        .respondentTitle(MRS)
                        .respondentSurname(PERSON5_SURNAME)
                        .respondentForename1(PERSON5_FORENAME1)
                        .respondentForename2(PERSON5_FORENAME2)
                        .respondentForename3(PERSON5_FORENAME3)
                        .respondentAddressLine1(PERSON5_ADDRESSLINE1)
                        .respondentAddressLine2(PERSON5_ADDRESSLINE2)
                        .respondentAddressLine3(PERSON5_ADDRESSLINE3)
                        .respondentAddressLine4(PERSON5_ADDRESSLINE4)
                        .respondentAddressLine5(PERSON5_ADDRESSLINE5)
                        .respondentPostcode(PERSON5_POSTCODE)
                        .respondentPhone(PERSON5_PHONE)
                        .respondentMobile(PERSON5_MOBILE)
                        .respondentEmail(PERSON5_EMAIL)
                        .respondentDateOfBirth(PERSON5_DATE_OF_BIRTH)
                        .applicationCode(APPLICATIONCODE1_CODE)
                        .applicationTitle(APPLICATIONCODE1_TITLE)
                        .applicationWording(APPLICATIONLISTENTRY1_WORDING)
                        .caseReference(APPLICATIONLISTENTRY1_CASEREFERENCE)
                        .accountReference(APPLICATIONLISTENTRY1_ACCOUNTNUMBER)
                        .notes(APPLICATIONLISTENTRY1_NOTES)
                        .build();

        var dto = new ApplicationListEntryMapStructMapperImpl().toPrintDto(projection);

        var applicant = dto.getApplicant().getPerson();
        var respondent = dto.getRespondent().getPerson();

        assertContactDetailsEqual(
                applicant.getContactDetails(),
                PERSON4_ADDRESSLINE1,
                PERSON4_ADDRESSLINE2,
                PERSON4_ADDRESSLINE3,
                PERSON4_ADDRESSLINE4,
                PERSON4_ADDRESSLINE5,
                PERSON4_POSTCODE,
                PERSON4_PHONE,
                PERSON4_MOBILE,
                PERSON4_EMAIL);

        assertContactDetailsEqual(
                respondent.getContactDetails(),
                PERSON5_ADDRESSLINE1,
                PERSON5_ADDRESSLINE2,
                PERSON5_ADDRESSLINE3,
                PERSON5_ADDRESSLINE4,
                PERSON5_ADDRESSLINE5,
                PERSON5_POSTCODE,
                PERSON5_PHONE,
                PERSON5_MOBILE,
                PERSON5_EMAIL);

        Assertions.assertEquals(MR, applicant.getName().getTitle());
        Assertions.assertEquals(PERSON4_SURNAME, applicant.getName().getSurname());
        Assertions.assertEquals(MRS, respondent.getName().getTitle());
        Assertions.assertEquals(PERSON5_SURNAME, respondent.getName().getSurname());
        Assertions.assertEquals(
                PERSON5_DATE_OF_BIRTH.toLocalDate(), dto.getRespondent().getDateOfBirth());

        assertApplicationDetailsEqual(dto);
    }

    @Test
    void testToPrintDto_provideOrganisations_validDtoGenerated() {
        var projection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicantAddressLine1(ORGANISATION1_ADDRESSLINE1)
                        .applicantAddressLine2(ORGANISATION1_ADDRESSLINE2)
                        .applicantAddressLine3(ORGANISATION1_ADDRESSLINE3)
                        .applicantAddressLine4(ORGANISATION1_ADDRESSLINE4)
                        .applicantAddressLine5(ORGANISATION1_ADDRESSLINE5)
                        .applicantPostcode(ORGANISATION1_POSTCODE)
                        .applicantPhone(ORGANISATION1_PHONE)
                        .applicantMobile(ORGANISATION1_MOBILE)
                        .applicantEmail(ORGANISATION1_EMAIL)
                        .applicantName(ORGANISATION1_NAME)
                        .respondentAddressLine1(ORGANISATION2_ADDRESSLINE1)
                        .respondentAddressLine2(ORGANISATION2_ADDRESSLINE2)
                        .respondentAddressLine3(ORGANISATION2_ADDRESSLINE3)
                        .respondentAddressLine4(ORGANISATION2_ADDRESSLINE4)
                        .respondentAddressLine5(ORGANISATION2_ADDRESSLINE5)
                        .respondentPostcode(ORGANISATION2_POSTCODE)
                        .respondentPhone(ORGANISATION2_PHONE)
                        .respondentMobile(ORGANISATION2_MOBILE)
                        .respondentEmail(ORGANISATION2_EMAIL)
                        .respondentName(ORGANISATION2_NAME)
                        .applicationCode(APPLICATIONCODE1_CODE)
                        .applicationTitle(APPLICATIONCODE1_TITLE)
                        .applicationWording(APPLICATIONLISTENTRY1_WORDING)
                        .caseReference(APPLICATIONLISTENTRY1_CASEREFERENCE)
                        .accountReference(APPLICATIONLISTENTRY1_ACCOUNTNUMBER)
                        .notes(APPLICATIONLISTENTRY1_NOTES)
                        .build();

        var dto = new ApplicationListEntryMapStructMapperImpl().toPrintDto(projection);

        assertContactDetailsEqual(
                dto.getApplicant().getOrganisation().getContactDetails(),
                ORGANISATION1_ADDRESSLINE1,
                ORGANISATION1_ADDRESSLINE2,
                ORGANISATION1_ADDRESSLINE3,
                ORGANISATION1_ADDRESSLINE4,
                ORGANISATION1_ADDRESSLINE5,
                ORGANISATION1_POSTCODE,
                ORGANISATION1_PHONE,
                ORGANISATION1_MOBILE,
                ORGANISATION1_EMAIL);

        assertContactDetailsEqual(
                dto.getRespondent().getOrganisation().getContactDetails(),
                ORGANISATION2_ADDRESSLINE1,
                ORGANISATION2_ADDRESSLINE2,
                ORGANISATION2_ADDRESSLINE3,
                ORGANISATION2_ADDRESSLINE4,
                ORGANISATION2_ADDRESSLINE5,
                ORGANISATION2_POSTCODE,
                ORGANISATION2_PHONE,
                ORGANISATION2_MOBILE,
                ORGANISATION2_EMAIL);

        assertApplicationDetailsEqual(dto);
    }

    private static void assertApplicationListEntrySummary(
            UUID uuid,
            int sequenceNumber,
            ApplicationListEntrySummary dto,
            String accountNumber,
            String applicant,
            String respondent,
            String postCode,
            String applicationTitle,
            boolean feeRequired,
            String result) {
        Assertions.assertEquals(uuid, dto.getUuid());
        Assertions.assertEquals(sequenceNumber, dto.getSequenceNumber());
        Assertions.assertEquals(accountNumber, dto.getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant, dto.getApplicant().orElse(null));
        Assertions.assertEquals(respondent, dto.getRespondent().orElse(null));
        Assertions.assertEquals(postCode, dto.getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle, dto.getApplicationTitle());
        Assertions.assertEquals(feeRequired, dto.getFeeRequired());
        Assertions.assertEquals(result, dto.getResult().orElse(null));
    }

    private void assertContactDetailsEqual(
            @NotNull @Valid ContactDetails actual,
            String line1,
            String line2,
            String line3,
            String line4,
            String line5,
            String postcode,
            String phone,
            String mobile,
            String email) {
        Assertions.assertEquals(line1, actual.getAddressLine1());
        Assertions.assertEquals(line2, actual.getAddressLine2());
        Assertions.assertEquals(line3, actual.getAddressLine3());
        Assertions.assertEquals(line4, actual.getAddressLine4());
        Assertions.assertEquals(line5, actual.getAddressLine5());
        Assertions.assertEquals(postcode, actual.getPostcode());
        Assertions.assertEquals(phone, actual.getPhone());
        Assertions.assertEquals(mobile, actual.getMobile());
        Assertions.assertEquals(email, actual.getEmail());
    }

    private void assertApplicationDetailsEqual(EntryGetPrintDto dto) {
        Assertions.assertEquals(APPLICATIONCODE1_CODE, dto.getApplicationCode());
        Assertions.assertEquals(APPLICATIONCODE1_TITLE, dto.getApplicationTitle());
        Assertions.assertEquals(APPLICATIONLISTENTRY1_WORDING, dto.getApplicationWording());
        Assertions.assertEquals(APPLICATIONLISTENTRY1_CASEREFERENCE, dto.getCaseReference());
        Assertions.assertEquals(APPLICATIONLISTENTRY1_ACCOUNTNUMBER, dto.getAccountReference());
        Assertions.assertEquals(APPLICATIONLISTENTRY1_NOTES, dto.getNotes());
    }
}
