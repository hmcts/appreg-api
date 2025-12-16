package uk.gov.hmcts.appregister.applicationentry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
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
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;

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
        Assertions.assertEquals(PERSON5_DATE_OF_BIRTH, dto.getRespondent().getDateOfBirth());

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
                        + "{TEXT|Applicant officer2|10} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|10}");

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
                OfficialType.CLERK, entryGetDetailDto.getOfficials().get(0).getType());
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
                OfficialType.MAGISTRATE, entryGetDetailDto.getOfficials().get(1).getType());
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
