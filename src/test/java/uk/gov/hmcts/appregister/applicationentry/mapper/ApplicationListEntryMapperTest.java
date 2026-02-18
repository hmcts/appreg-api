package uk.gov.hmcts.appregister.applicationentry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.util.ApplicationListEntryPrintProjectionUtil.applicationListEntryPrintProjection;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryFeeStatusTestData;
import uk.gov.hmcts.appregister.data.AppListEntryOfficialTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionBuilder;

class ApplicationListEntryMapperTest {

    private ApplicationListEntryMapper mapper;

    @BeforeEach
    void beforeEach() {
        mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setOfficialMapper(new OfficialMapper());
    }

    @Test
    void testToSummaryModel_provideValidData_validModelGenerated() {
        NameAddress applicant = new NameAddress();
        applicant.setName("Mustafa's Org");

        NameAddress respondent = new NameAddress();
        respondent.setTitle("His Majesty");
        respondent.setForename1("Ahmed");
        respondent.setSurname("Mustafa");

        var postCode = "SW1A 1AA";
        var applicationTitle = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired = true;
        var result = "APPC";

        var uuid = UUID.randomUUID();
        short sequenceNumber = 1;
        var accountNumber = "1234567890";

        var projection =
                ApplicationListEntrySummaryProjectionBuilder.builder()
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
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        var model = mapper.toSummaryDto(projection);

        assertApplicationListEntrySummary(
                uuid,
                sequenceNumber,
                model,
                accountNumber,
                "Mustafa's Org",
                "Mustafa, Ahmed, His Majesty",
                postCode,
                applicationTitle,
                feeRequired,
                result);
    }

    @Test
    void testToSummaryModelList_provideValidData_validModelListGenerated() {
        NameAddress applicant1 = new NameAddress();
        applicant1.setName("Mustafa's Org");

        NameAddress respondent1 = new NameAddress();
        respondent1.setTitle("His Majesty");
        respondent1.setSurname("Mustafa");
        respondent1.setForename1("Ahmed");

        NameAddress applicant2 = new NameAddress();
        applicant2.setName("Mustafa's Org");

        NameAddress respondent2 = new NameAddress();
        respondent2.setForename1("Sarah");
        respondent2.setSurname("Johnson");

        var accountNumber2 = "1234567891";
        var postCode2 = "EH1 3QR";
        var applicationTitle2 = "Copy documents";
        var feeRequired2 = false;
        var result2 = "RESP";

        var uuid2 = UUID.randomUUID();
        short sequenceNumber2 = 2;

        var projection2 =
                ApplicationListEntrySummaryProjectionBuilder.builder()
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
        mapper.setApplicantMapper(new ApplicantMapperImpl());

        var uuid1 = UUID.randomUUID();
        short sequenceNumber1 = 1;
        var accountNumber1 = "1234567890";

        var postCode1 = "SW1A 1AA";
        var applicationTitle1 = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired1 = true;
        var result1 = "APPC";

        var projection1 =
                ApplicationListEntrySummaryProjectionBuilder.builder()
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

        List<ApplicationListEntrySummary> list =
                mapper.toSummaryDtoList(List.of(projection1, projection2));

        assertThat(list).hasSize(2);

        assertApplicationListEntrySummary(
                uuid1,
                sequenceNumber1,
                list.getFirst(),
                accountNumber1,
                "Mustafa's Org",
                "Mustafa, Ahmed, His Majesty",
                postCode1,
                applicationTitle1,
                feeRequired1,
                result1);

        assertApplicationListEntrySummary(
                uuid2,
                sequenceNumber2,
                list.getLast(),
                accountNumber2,
                "Mustafa's Org",
                "Johnson, Sarah",
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

        var dto = new ApplicationListEntryMapperImpl().toPrintDto(projection);

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
                PERSON5_DATE_OF_BIRTH, dto.getRespondent().getPerson().getDateOfBirth());

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

        var dto = new ApplicationListEntryMapperImpl().toPrintDto(projection);

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

    @Test
    public void toEntrySummary() {
        // the applicant does have a name so is an organisation
        NameAddress applicant = new NameAddress();
        applicant.setName("name");
        applicant.setCode(NameAddressCodeType.APPLICANT);
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
        respondent.setCode(NameAddressCodeType.RESPONDENT);
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

        UUID listId = UUID.randomUUID();
        when(applicationListEntryGetSummaryProjection.getListId()).thenReturn(listId.toString());

        when(applicationListEntryGetSummaryProjection.getDateOfAl()).thenReturn(LocalDate.now());

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
        Assertions.assertEquals(listId.toString(), mappedResult.getListId().toString());
        Assertions.assertEquals(LocalDate.now(), mappedResult.getDate());
    }

    @Test
    void toEntryGetDetailDto_provideValidData_validModelListGenerated() {
        NameAddressTestData nameAddressTestData = new NameAddressTestData();
        NameAddress applicant = nameAddressTestData.somePerson();
        NameAddress respondent = nameAddressTestData.someOrganisation();
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();

        // create the entity data to use
        ApplicationListEntry appListEntry = appListEntryTestData.someComplete();
        appListEntry.setRnameaddress(respondent);
        appListEntry.setAnamedaddress(applicant);

        ApplicationCode code = applicationCodeTestData.someComplete();
        code.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template "
                        + "{TEXT|Applicant officer2|10} and third template {TEXT|Applicant officer3|10}");

        AppListEntryFeeStatusTestData statusTestData = new AppListEntryFeeStatusTestData();

        appListEntry.setApplicationCode(code);
        AppListEntryFeeStatus applicationListStatus = statusTestData.someComplete();
        AppListEntryFeeStatus applicationListStatus2 = statusTestData.someComplete();

        applicationListStatus.setAlefsFeeStatus(FeeStatusType.PAID);
        applicationListStatus2.setAlefsFeeStatus(FeeStatusType.REMITTED);

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();

        FeeTestData feeTestData = new FeeTestData();
        AppListEntryOfficial appListEntryOfficial = officialTestData.someComplete();
        appListEntryOfficial.setOfficialType(
                uk.gov.hmcts.appregister.common.enumeration.OfficialType.CLERK);

        AppListEntryOfficial appListEntryOfficial2 = officialTestData.someComplete();
        appListEntryOfficial2.setOfficialType(
                uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE);

        Fee fee = feeTestData.someComplete();

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        EntryGetDetailDto entryGetDetailDto =
                mapper.toEntryGetDetailDto(
                        appListEntry,
                        List.of(applicationListStatus, applicationListStatus2),
                        fee,
                        List.of(appListEntryOfficial, appListEntryOfficial2),
                        null);

        // assert on the main application list entry data
        Assertions.assertEquals(
                appListEntry.getCaseReference(), entryGetDetailDto.getCaseReference());
        Assertions.assertEquals(appListEntry.getNotes(), entryGetDetailDto.getNotes());
        Assertions.assertEquals(
                appListEntry.getAccountNumber(), entryGetDetailDto.getAccountNumber());
        Assertions.assertEquals(
                appListEntry.getApplicationCode().getCode(),
                entryGetDetailDto.getApplicationCode());
        Assertions.assertEquals(
                appListEntry.getStandardApplicant().getApplicantCode(),
                entryGetDetailDto.getStandardApplicantCode());

        // assert the applicant data
        Assertions.assertEquals(
                applicant.getSurname(),
                entryGetDetailDto.getApplicant().getPerson().getName().getSurname());
        Assertions.assertEquals(
                applicant.getForename1(),
                entryGetDetailDto.getApplicant().getPerson().getName().getFirstForename());
        Assertions.assertEquals(
                applicant.getForename2(),
                entryGetDetailDto.getApplicant().getPerson().getName().getSecondForename());
        Assertions.assertEquals(
                applicant.getForename3(),
                entryGetDetailDto.getApplicant().getPerson().getName().getThirdForename());
        Assertions.assertEquals(
                applicant.getTitle(),
                entryGetDetailDto.getApplicant().getPerson().getName().getTitle());
        Assertions.assertEquals(
                applicant.getMobileNumber(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getMobile());
        Assertions.assertEquals(
                applicant.getEmailAddress(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getEmail());
        Assertions.assertEquals(
                applicant.getPostcode(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                applicant.getTelephoneNumber(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getPhone());
        Assertions.assertEquals(
                applicant.getAddress1(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                applicant.getAddress2(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                applicant.getAddress3(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                applicant.getAddress4(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                applicant.getAddress5(),
                entryGetDetailDto.getApplicant().getPerson().getContactDetails().getAddressLine5());

        // assert the respondent details
        Assertions.assertEquals(
                respondent.getName(),
                entryGetDetailDto.getRespondent().getOrganisation().getName());
        Assertions.assertEquals(
                respondent.getMobileNumber(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getMobile());
        Assertions.assertEquals(
                respondent.getEmailAddress(),
                entryGetDetailDto.getRespondent().getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                respondent.getPostcode(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getPostcode());
        Assertions.assertEquals(
                respondent.getTelephoneNumber(),
                entryGetDetailDto.getRespondent().getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                respondent.getAddress1(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine1());
        Assertions.assertEquals(
                respondent.getAddress2(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine2());
        Assertions.assertEquals(
                respondent.getAddress3(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine3());
        Assertions.assertEquals(
                respondent.getAddress4(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine4());
        Assertions.assertEquals(
                respondent.getAddress5(),
                entryGetDetailDto
                        .getRespondent()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine5());

        Assertions.assertEquals(3, entryGetDetailDto.getWordingFields().size());
        Assertions.assertEquals("Applicant officer1", entryGetDetailDto.getWordingFields().get(0));
        Assertions.assertEquals("Applicant officer2", entryGetDetailDto.getWordingFields().get(1));
        Assertions.assertEquals("Applicant officer3", entryGetDetailDto.getWordingFields().get(2));

        Assertions.assertEquals(2, entryGetDetailDto.getOfficials().size());
        Assertions.assertEquals(
                appListEntryOfficial.getSurname(),
                entryGetDetailDto.getOfficials().get(0).getSurname());
        Assertions.assertEquals(
                appListEntryOfficial.getForename(),
                entryGetDetailDto.getOfficials().get(0).getForename());
        Assertions.assertEquals(
                mapper.officialMapper.toOfficial(OfficialType.CLERK),
                entryGetDetailDto.getOfficials().get(0).getType());
        Assertions.assertEquals(
                appListEntryOfficial.getTitle(),
                entryGetDetailDto.getOfficials().get(0).getTitle());

        Assertions.assertEquals(
                appListEntryOfficial2.getSurname(),
                entryGetDetailDto.getOfficials().get(1).getSurname());
        Assertions.assertEquals(
                appListEntryOfficial2.getForename(),
                entryGetDetailDto.getOfficials().get(1).getForename());
        Assertions.assertEquals(
                mapper.officialMapper.toOfficial(OfficialType.MAGISTRATE),
                entryGetDetailDto.getOfficials().get(1).getType());
        Assertions.assertEquals(
                appListEntryOfficial2.getTitle(),
                entryGetDetailDto.getOfficials().get(1).getTitle());

        // assert the fee status details
        Assertions.assertEquals(2, entryGetDetailDto.getFeeStatuses().size());

        Assertions.assertEquals(
                applicationListStatus.getAlefsPaymentReference(),
                entryGetDetailDto.getFeeStatuses().get(0).getPaymentReference());
        Assertions.assertEquals(
                PaymentStatus.PAID, entryGetDetailDto.getFeeStatuses().get(0).getPaymentStatus());

        Assertions.assertEquals(
                applicationListStatus2.getAlefsPaymentReference(),
                entryGetDetailDto.getFeeStatuses().get(1).getPaymentReference());
        Assertions.assertEquals(
                PaymentStatus.REMITTED,
                entryGetDetailDto.getFeeStatuses().get(1).getPaymentStatus());
    }

    @Test
    void
            toEntryGetDetailDtoApplicantPersonRespondentOrg_provideValidData_validModelListGenerated() {
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        // create the entity data to use
        ApplicationListEntry appListEntry = appListEntryTestData.someComplete();

        // make sure we generate person
        appListEntry.getAnamedaddress().setName(null);
        appListEntry.getRnameaddress().setName("Some Organisation");
        appListEntry.setStandardApplicant(null);

        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();

        ApplicationCode code = applicationCodeTestData.someComplete();
        code.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template "
                        + "{TEXT|Applicant officer2|10} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|10}");

        appListEntry.setApplicationCode(code);

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        EntryGetDetailDto entryGetDetailDto = mapper.toEntryGetDetailDto(appListEntry, false);

        // assert on the main application list entry data
        Assertions.assertEquals(
                appListEntry.getCaseReference(), entryGetDetailDto.getCaseReference());
        Assertions.assertEquals(appListEntry.getNotes(), entryGetDetailDto.getNotes());
        Assertions.assertEquals(
                appListEntry.getAccountNumber(), entryGetDetailDto.getAccountNumber());
        Assertions.assertEquals(
                appListEntry.getApplicationCode().getCode(),
                entryGetDetailDto.getApplicationCode());
        Assertions.assertNull(entryGetDetailDto.getStandardApplicantCode());
        Assertions.assertEquals(
                appListEntry.getLodgementDate(), entryGetDetailDto.getLodgementDate());

        // validate the applicant and organisation
        validateApplicantPerson(appListEntry.getAnamedaddress(), entryGetDetailDto.getApplicant());
        validateRespondentOrganisation(
                appListEntry.getRnameaddress(), entryGetDetailDto.getRespondent());

        // validate the wording
        Assertions.assertEquals(3, entryGetDetailDto.getWordingFields().size());
        Assertions.assertEquals("Applicant officer1", entryGetDetailDto.getWordingFields().get(0));
        Assertions.assertEquals("Applicant officer2", entryGetDetailDto.getWordingFields().get(1));
        Assertions.assertEquals("Applicant officer3", entryGetDetailDto.getWordingFields().get(2));

        // validate the officials
        Assertions.assertFalse(entryGetDetailDto.getOfficials().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getSurname(),
                    entryGetDetailDto.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getForename(),
                    entryGetDetailDto.getOfficials().get(i).getForename());
            Assertions.assertEquals(
                    mapper.officialMapper.toOfficial(
                            appListEntry.getOfficials().get(i).getOfficialType()),
                    entryGetDetailDto.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getTitle(),
                    entryGetDetailDto.getOfficials().get(i).getTitle());
        }

        // validate the statuses
        Assertions.assertFalse(entryGetDetailDto.getFeeStatuses().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getEntryFeeStatuses().get(i).getAlefsPaymentReference(),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    mapper.getStatus(appListEntry.getEntryFeeStatuses().get(i).getAlefsFeeStatus()),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentStatus());
        }
    }

    @Test
    void
            toEntryGetDetailDtoApplicantOrgRespondentPerson_provideValidData_validModelListGenerated() {
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        // create the entity data to use
        ApplicationListEntry appListEntry = appListEntryTestData.someComplete();

        // make sure we generate person
        appListEntry.getAnamedaddress().setName("Some Organisation");
        appListEntry.getRnameaddress().setName(null);
        appListEntry.setStandardApplicant(null);

        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();

        ApplicationCode code = applicationCodeTestData.someComplete();
        code.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template "
                        + "{TEXT|Applicant officer2|10} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|10}");

        appListEntry.setApplicationCode(code);

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        EntryGetDetailDto entryGetDetailDto = mapper.toEntryGetDetailDto(appListEntry, false);

        // assert on the main application list entry data
        Assertions.assertEquals(
                appListEntry.getCaseReference(), entryGetDetailDto.getCaseReference());
        Assertions.assertEquals(appListEntry.getNotes(), entryGetDetailDto.getNotes());
        Assertions.assertEquals(
                appListEntry.getAccountNumber(), entryGetDetailDto.getAccountNumber());
        Assertions.assertEquals(
                appListEntry.getApplicationCode().getCode(),
                entryGetDetailDto.getApplicationCode());
        Assertions.assertNull(entryGetDetailDto.getStandardApplicantCode());
        Assertions.assertEquals(
                appListEntry.getLodgementDate(), entryGetDetailDto.getLodgementDate());

        // validate the applicant and organisation
        validateApplicantOrganisation(
                appListEntry.getAnamedaddress(), entryGetDetailDto.getApplicant());
        validateRespondentPerson(appListEntry.getRnameaddress(), entryGetDetailDto.getRespondent());

        // validate the wording
        Assertions.assertEquals(3, entryGetDetailDto.getWordingFields().size());
        Assertions.assertEquals("Applicant officer1", entryGetDetailDto.getWordingFields().get(0));
        Assertions.assertEquals("Applicant officer2", entryGetDetailDto.getWordingFields().get(1));
        Assertions.assertEquals("Applicant officer3", entryGetDetailDto.getWordingFields().get(2));

        // validate the officials
        Assertions.assertFalse(entryGetDetailDto.getOfficials().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getSurname(),
                    entryGetDetailDto.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getForename(),
                    entryGetDetailDto.getOfficials().get(i).getForename());
            Assertions.assertEquals(
                    mapper.officialMapper.toOfficial(
                            appListEntry.getOfficials().get(i).getOfficialType()),
                    entryGetDetailDto.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getTitle(),
                    entryGetDetailDto.getOfficials().get(i).getTitle());
        }

        // validate the statuses
        Assertions.assertFalse(entryGetDetailDto.getFeeStatuses().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getEntryFeeStatuses().get(i).getAlefsPaymentReference(),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    mapper.getStatus(appListEntry.getEntryFeeStatuses().get(i).getAlefsFeeStatus()),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentStatus());
        }
    }

    @Test
    void
            toEntryGetDetailDtoStandardApplicantOrgRespondentPerson_provideValidData_validModelListGenerated() {
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        // create the entity data to use
        ApplicationListEntry appListEntry = appListEntryTestData.someComplete();

        // make sure we generate person
        appListEntry.setAnamedaddress(null);
        appListEntry.getRnameaddress().setName(null);
        appListEntry.getStandardApplicant().setName("Some Organisation");

        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();

        ApplicationCode code = applicationCodeTestData.someComplete();
        code.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template "
                        + "{TEXT|Applicant officer2|10} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|10}");

        appListEntry.setApplicationCode(code);

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        EntryGetDetailDto entryGetDetailDto = mapper.toEntryGetDetailDto(appListEntry, false);

        // assert on the main application list entry data
        Assertions.assertEquals(
                appListEntry.getCaseReference(), entryGetDetailDto.getCaseReference());
        Assertions.assertEquals(appListEntry.getNotes(), entryGetDetailDto.getNotes());
        Assertions.assertEquals(
                appListEntry.getAccountNumber(), entryGetDetailDto.getAccountNumber());
        Assertions.assertEquals(
                appListEntry.getApplicationCode().getCode(),
                entryGetDetailDto.getApplicationCode());
        Assertions.assertNotNull(entryGetDetailDto.getStandardApplicantCode());
        Assertions.assertEquals(
                appListEntry.getLodgementDate(), entryGetDetailDto.getLodgementDate());

        // validate the applicant and organisation
        validateApplicantOrganisation(
                appListEntry.getStandardApplicant(), entryGetDetailDto.getApplicant());
        validateRespondentPerson(appListEntry.getRnameaddress(), entryGetDetailDto.getRespondent());

        // validate the wording
        Assertions.assertEquals(3, entryGetDetailDto.getWordingFields().size());
        Assertions.assertEquals("Applicant officer1", entryGetDetailDto.getWordingFields().get(0));
        Assertions.assertEquals("Applicant officer2", entryGetDetailDto.getWordingFields().get(1));
        Assertions.assertEquals("Applicant officer3", entryGetDetailDto.getWordingFields().get(2));

        // validate the officials
        Assertions.assertFalse(entryGetDetailDto.getOfficials().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getSurname(),
                    entryGetDetailDto.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getForename(),
                    entryGetDetailDto.getOfficials().get(i).getForename());
            Assertions.assertEquals(
                    mapper.officialMapper.toOfficial(
                            appListEntry.getOfficials().get(i).getOfficialType()),
                    entryGetDetailDto.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    appListEntry.getOfficials().get(i).getTitle(),
                    entryGetDetailDto.getOfficials().get(i).getTitle());
        }

        // validate the statuses
        Assertions.assertFalse(entryGetDetailDto.getFeeStatuses().isEmpty());
        for (int i = 0; i < entryGetDetailDto.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    appListEntry.getEntryFeeStatuses().get(i).getAlefsPaymentReference(),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    mapper.getStatus(appListEntry.getEntryFeeStatuses().get(i).getAlefsFeeStatus()),
                    entryGetDetailDto.getFeeStatuses().get(i).getPaymentStatus());
        }
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

    private void validateApplicantPerson(NameAddress entity, Applicant applicant) {
        // assert the applicant data
        Assertions.assertNotNull(applicant.getPerson());
        Assertions.assertEquals(entity.getSurname(), applicant.getPerson().getName().getSurname());
        Assertions.assertEquals(
                entity.getForename1(), applicant.getPerson().getName().getFirstForename());
        Assertions.assertEquals(
                entity.getForename2(), applicant.getPerson().getName().getSecondForename());
        Assertions.assertEquals(
                entity.getForename3(), applicant.getPerson().getName().getThirdForename());
        Assertions.assertEquals(entity.getTitle(), applicant.getPerson().getName().getTitle());
        Assertions.assertEquals(
                entity.getMobileNumber(), applicant.getPerson().getContactDetails().getMobile());
        Assertions.assertEquals(
                entity.getEmailAddress(), applicant.getPerson().getContactDetails().getEmail());
        Assertions.assertEquals(
                entity.getPostcode(), applicant.getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(), applicant.getPerson().getContactDetails().getPhone());
        Assertions.assertEquals(
                entity.getAddress1(), applicant.getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(), applicant.getPerson().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                entity.getAddress3(), applicant.getPerson().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                entity.getAddress4(), applicant.getPerson().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                entity.getAddress5(), applicant.getPerson().getContactDetails().getAddressLine5());
    }

    private void validateApplicantOrganisation(NameAddress entity, Applicant applicant) {
        // assert the applicant data
        Assertions.assertNotNull(applicant.getOrganisation());
        Assertions.assertEquals(entity.getName(), applicant.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                applicant.getOrganisation().getContactDetails().getMobile());
        Assertions.assertEquals(
                entity.getEmailAddress(),
                applicant.getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                entity.getPostcode(),
                applicant.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                applicant.getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                entity.getAddress1(),
                applicant.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                applicant.getOrganisation().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                entity.getAddress3(),
                applicant.getOrganisation().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                entity.getAddress4(),
                applicant.getOrganisation().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                entity.getAddress5(),
                applicant.getOrganisation().getContactDetails().getAddressLine5());
    }

    private void validateApplicantOrganisation(StandardApplicant entity, Applicant applicant) {
        // assert the applicant data
        Assertions.assertNotNull(applicant.getOrganisation());
        Assertions.assertEquals(entity.getName(), applicant.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                applicant.getOrganisation().getContactDetails().getMobile());
        Assertions.assertEquals(
                entity.getEmailAddress(),
                applicant.getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                entity.getPostcode(),
                applicant.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                applicant.getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                entity.getAddressLine1(),
                applicant.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddressLine2(),
                applicant.getOrganisation().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                entity.getAddressLine3(),
                applicant.getOrganisation().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                entity.getAddressLine4(),
                applicant.getOrganisation().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                entity.getAddressLine5(),
                applicant.getOrganisation().getContactDetails().getAddressLine5());
    }

    private void validateRespondentPerson(NameAddress entity, Respondent respondent) {
        // assert the applicant data
        Assertions.assertNotNull(respondent.getPerson());
        Assertions.assertEquals(entity.getSurname(), respondent.getPerson().getName().getSurname());
        Assertions.assertEquals(
                entity.getForename1(), respondent.getPerson().getName().getFirstForename());
        Assertions.assertEquals(
                entity.getForename2(), respondent.getPerson().getName().getSecondForename());
        Assertions.assertEquals(
                entity.getForename3(), respondent.getPerson().getName().getThirdForename());
        Assertions.assertEquals(entity.getTitle(), respondent.getPerson().getName().getTitle());
        Assertions.assertEquals(
                entity.getMobileNumber(), respondent.getPerson().getContactDetails().getMobile());
        Assertions.assertEquals(
                entity.getEmailAddress(), respondent.getPerson().getContactDetails().getEmail());
        Assertions.assertEquals(
                entity.getPostcode(), respondent.getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(), respondent.getPerson().getContactDetails().getPhone());
        Assertions.assertEquals(
                entity.getAddress1(), respondent.getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(), respondent.getPerson().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                entity.getAddress3(), respondent.getPerson().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                entity.getAddress4(), respondent.getPerson().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                entity.getAddress5(), respondent.getPerson().getContactDetails().getAddressLine5());
        Assertions.assertEquals(entity.getDateOfBirth(), respondent.getPerson().getDateOfBirth());
    }

    private void validateRespondentOrganisation(NameAddress entity, Respondent respondent) {
        // assert the applicant data
        Assertions.assertNotNull(respondent.getOrganisation());
        Assertions.assertEquals(entity.getName(), respondent.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                respondent.getOrganisation().getContactDetails().getMobile());
        Assertions.assertEquals(
                entity.getEmailAddress(),
                respondent.getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                entity.getPostcode(),
                respondent.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                respondent.getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                entity.getAddress1(),
                respondent.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                respondent.getOrganisation().getContactDetails().getAddressLine2());
        Assertions.assertEquals(
                entity.getAddress3(),
                respondent.getOrganisation().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                entity.getAddress4(),
                respondent.getOrganisation().getContactDetails().getAddressLine4());
        Assertions.assertEquals(
                entity.getAddress5(),
                respondent.getOrganisation().getContactDetails().getAddressLine5());
    }
}
