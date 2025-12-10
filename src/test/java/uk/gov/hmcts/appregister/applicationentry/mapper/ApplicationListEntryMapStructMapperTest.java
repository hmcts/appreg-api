package uk.gov.hmcts.appregister.applicationentry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionUtil.applicationListEntrySummaryProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;

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

        var mapper = new ApplicationListEntryMapperImpl();
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

        var mapper = new ApplicationListEntryMapperImpl();
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
    public void toEntrySummary() {
        // the applicant does have a name so is an organisation
        NameAddress applicant = new NameAddress();
        applicant.setName("name");
        applicant.setCode("acode");
        applicant.setAddress1("aaddress1");
        applicant.setAddress2("aaddress2");
        applicant.setAddress3("aaddress3");
        applicant.setAddress4("aaddress4");
        applicant.setAddress5("aaddress5");
        applicant.setEmailAddress("aemail");
        applicant.setTelephoneNumber("atel");
        applicant.setMobileNumber("amobile");
        applicant.setPostcode("apostcode");

        // the respondent is a person
        NameAddress respondent = new NameAddress();
        respondent.setSurname("rsurname");
        respondent.setCode("rcode");
        respondent.setAddress1("raddress1");
        respondent.setAddress2("raddress2");
        respondent.setAddress3("raddress3");
        respondent.setAddress4("raddress4");
        respondent.setAddress5("raddress5");
        respondent.setEmailAddress("remail");
        respondent.setTelephoneNumber("rtel");
        respondent.setMobileNumber("rmobile");
        respondent.setPostcode("rpostcode");
        respondent.setForename1("rforename1");
        respondent.setForename2("rforename2");
        respondent.setForename3("rforename3");

        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameAddress()).thenReturn(applicant);
        when(applicationListEntryGetSummaryProjection.getRnameAddress()).thenReturn(respondent);
        when(applicationListEntryGetSummaryProjection.getDateOfAl()).thenReturn(LocalDate.now());

        when(applicationListEntryGetSummaryProjection.getAccountReference()).thenReturn("accref");
        when(applicationListEntryGetSummaryProjection.getCjaCode()).thenReturn("cjacode");
        when(applicationListEntryGetSummaryProjection.getCourtCode()).thenReturn("courtcode");
        when(applicationListEntryGetSummaryProjection.getLegislation()).thenReturn("leg");
        when(applicationListEntryGetSummaryProjection.getTitle()).thenReturn("title");

        when(applicationListEntryGetSummaryProjection.getRespondentSurname())
                .thenReturn("ressurname");
        when(applicationListEntryGetSummaryProjection.getResult()).thenReturn("2");
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.CLOSED);

        UUID uuidForProjection = UUID.randomUUID();
        when(applicationListEntryGetSummaryProjection.getUuid())
                .thenReturn(uuidForProjection.toString());

        var mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());

        // run test
        EntryGetSummaryDto mappedResult =
                mapper.toEntrySummary(applicationListEntryGetSummaryProjection);

        // assert
        Assertions.assertEquals(ApplicationListStatus.CLOSED, mappedResult.getStatus());
        Assertions.assertEquals("leg", mappedResult.getLegislation());
        Assertions.assertEquals("title", mappedResult.getApplicationTitle());
        Assertions.assertEquals("name", mappedResult.getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                "aaddress1",
                mappedResult
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine1());
        Assertions.assertEquals(
                "aaddress2",
                mappedResult
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine2());
        Assertions.assertEquals(
                "aaddress3",
                mappedResult
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine3());
        Assertions.assertEquals(
                "aaddress4",
                mappedResult
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine4());
        Assertions.assertEquals(
                "aaddress5",
                mappedResult
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine5());
        Assertions.assertEquals(
                "atel",
                mappedResult.getApplicant().getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                "apostcode",
                mappedResult.getApplicant().getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                "aemail",
                mappedResult.getApplicant().getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                "amobile",
                mappedResult.getApplicant().getOrganisation().getContactDetails().getMobile());
        Assertions.assertEquals(
                "raddress1",
                mappedResult.getRespondent().getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                "raddress2",
                mappedResult.getRespondent().getPerson().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                "raddress3",
                mappedResult.getRespondent().getPerson().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                "raddress4",
                mappedResult.getRespondent().getPerson().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                "raddress5",
                mappedResult.getRespondent().getPerson().getContactDetails().getAddressLine5());
        Assertions.assertEquals(
                "rtel", mappedResult.getRespondent().getPerson().getContactDetails().getPhone());
        Assertions.assertEquals(
                "rpostcode",
                mappedResult.getRespondent().getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                "remail", mappedResult.getRespondent().getPerson().getContactDetails().getEmail());
        Assertions.assertEquals(
                "rmobile",
                mappedResult.getRespondent().getPerson().getContactDetails().getMobile());
        Assertions.assertEquals(
                "rsurname", mappedResult.getRespondent().getPerson().getName().getSurname());
        Assertions.assertEquals(
                "rforename3",
                mappedResult.getRespondent().getPerson().getName().getThirdForename());
        Assertions.assertEquals(
                "rforename1",
                mappedResult.getRespondent().getPerson().getName().getFirstForename());
        Assertions.assertEquals(
                "rforename2",
                mappedResult.getRespondent().getPerson().getName().getSecondForename());
        Assertions.assertTrue(mappedResult.getIsResulted());
        Assertions.assertFalse(mappedResult.getIsFeeRequired());
        Assertions.assertEquals(ApplicationListStatus.CLOSED, mappedResult.getStatus());
        Assertions.assertEquals(uuidForProjection.toString(), mappedResult.getId().toString());
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
}
