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
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryFeeStatusTestData;
import uk.gov.hmcts.appregister.data.AppListEntryOfficialTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@SuppressWarnings({"deprecation", "java:S1874"})
class ApplicationListEntryMapperTest {
    private static final LocalDate APPLICATION_LIST_DATE = LocalDate.of(2025, Month.OCTOBER, 7);

    private ApplicationListEntryMapper mapper;
    private final OfficialMapper officialMapper = new OfficialMapper();

    @BeforeEach
    void beforeEach() {
        mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setOfficialMapper(officialMapper);
    }

    @Test
    void testToEntryCreateDto_mapsBulkUploadApplicationTextColumnsInOrder() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("APP123");
        row.setRespondentOrganisationName("Respondent organisation");

        var applicationTexts = new ArrayListValuedHashMap<String, String>();
        applicationTexts.put("APPLICATION_TEXT2", "two");
        applicationTexts.put("APPLICATION_TEXT1", "one");
        applicationTexts.put("APPLICATION_TEXT3", "");
        row.setApplicationTexts(applicationTexts);

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        assertThat(dto.getWordingFields())
                .extracting(TemplateSubstitution::getValue)
                .containsExactly("one", "two", "");
    }

    @Test
    void givenBlankAccountNumber_whenMappingBulkUpload_thenMapsItToNull() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("SW99063");
        row.setAccountNumber("");
        row.setRespondentOrganisationName("Respondent organisation");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        assertThat(dto.getAccountNumber()).isNull();
    }

    @Test
    void testToEntryCreateDto_mapsBlankBulkUploadOrganisationAsPersonRespondent() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("APP123");
        row.setRespondentOrganisationName("");
        row.setRespondentTitle("Ms");
        row.setRespondentForename1("Beatrice");
        row.setRespondentForename2("Anne");
        row.setRespondentForename3("Louise");
        row.setRespondentSurname("Baxter");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        assertThat(dto.getRespondent().getOrganisation()).isNull();
        assertThat(dto.getRespondent().getPerson()).isNotNull();
        assertThat(dto.getRespondent().getPerson().getName().getTitle()).isEqualTo("Ms");
        assertThat(dto.getRespondent().getPerson().getName().getFirstName()).isEqualTo("Beatrice");
        assertThat(dto.getRespondent().getPerson().getName().getMiddleName())
                .isEqualTo(JsonNullable.of("Anne Louise"));
        assertThat(dto.getRespondent().getPerson().getName().getLastName()).isEqualTo("Baxter");
    }

    @Test
    void givenNoRespondentDetails_whenMappingBulkUpload_thenMapsRespondentToNull() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("MS99001");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        assertThat(dto.getRespondent()).isNull();
    }

    @Test
    void givenOnlyRespondentContactDetails_whenMappingBulkUpload_thenPreservesPartialRespondent() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("MS99001");
        row.setRespondentAddressLine1("1 Example Street");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        assertThat(dto.getRespondent().getPerson().getContactDetails().getAddressLine1())
                .isEqualTo("1 Example Street");
    }

    @Test
    void givenBlankOptionalContactFields_whenMappingBulkUpload_thenMapsThemToNull() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("APP123");
        row.setRespondentOrganisationName("Respondent organisation");
        row.setRespondentPostcode("");
        row.setRespondentTelephone("");
        row.setRespondentMobile("");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        var contactDetails = dto.getRespondent().getOrganisation().getContactDetails();
        assertThat(contactDetails.getPostcode()).isNull();
        assertThat(contactDetails.getPhone()).isEqualTo(JsonNullable.of(null));
        assertThat(contactDetails.getMobile()).isEqualTo(JsonNullable.of(null));
    }

    @Test
    void testToEntryCreateDto_mapsCanonicalBulkUploadPersonRespondent() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("APP123");
        row.setRespondentTitle("Ms");
        row.setRespondentFirstName("Beatrice");
        row.setRespondentMiddleName("Anne Louise");
        row.setRespondentLastName("Baxter");

        EntryCreateDto dto = mapper.toEntryCreateDto(row);

        var name = dto.getRespondent().getPerson().getName();
        assertThat(name.getTitle()).isEqualTo("Ms");
        assertThat(name.getFirstName()).isEqualTo("Beatrice");
        assertThat(name.getMiddleName()).isEqualTo(JsonNullable.of("Anne Louise"));
        assertThat(name.getLastName()).isEqualTo("Baxter");
    }

    @Test
    void testToApplicationListEntryForListReadAudit_mapsDbBackedFilters() {
        // Build the same path parameter + filter pair that the list-entry read endpoint receives.
        val listId = UUID.randomUUID();
        val payload = PayloadGetEntryInList.builder().listId(listId).build();

        val filterDto = new EntryApplicationListGetFilterDto();
        filterDto.setApplicantName("Applicant Audit Org");
        filterDto.setRespondentName("Respondent Audit Org");
        filterDto.setRespondentPostcode("ZZ1 1ZZ");
        filterDto.setAccountReference("ACC-123");
        filterDto.setApplicationTitle("Read audit application title");
        filterDto.setFeeRequired(Boolean.TRUE);
        filterDto.setSequenceNumber(7);

        // Map into the existing ApplicationListEntry audit surrogate rather than inventing a
        // separate audit-only type.
        val mappedResult = mapper.toApplicationListEntry(payload, filterDto);

        // Each assertion below corresponds to a database-backed field that should be available to
        // the reflective auditor when a GET /application-lists/{listId}/entries request succeeds.
        Assertions.assertEquals(0L, mappedResult.getId());
        Assertions.assertEquals(listId, mappedResult.getApplicationList().getUuid());
        Assertions.assertEquals("Applicant Audit Org", mappedResult.getAnamedaddress().getName());
        Assertions.assertEquals("Respondent Audit Org", mappedResult.getRnameaddress().getName());
        Assertions.assertEquals("ZZ1 1ZZ", mappedResult.getRnameaddress().getPostcode());
        Assertions.assertEquals("ACC-123", mappedResult.getAccountNumber());
        Assertions.assertEquals(
                "Read audit application title", mappedResult.getApplicationCode().getTitle());
        Assertions.assertEquals(YesOrNo.YES, mappedResult.getApplicationCode().getFeeDue());
        Assertions.assertEquals(Short.valueOf((short) 7), mappedResult.getSequenceNumber());
    }

    @Test
    void testToApplicationListEntry_mapsAuditEntryFilterDto() {
        var filterDto = new EntryGetFilterDto();
        filterDto.setAccountReference("ACC-456");
        filterDto.setStandardApplicantCode("STD001");
        filterDto.setApplicantOrganisation("Applicant Org");
        filterDto.setApplicantSurname("Applicant Surname");
        filterDto.setRespondentOrganisation("Respondent Org");
        filterDto.setRespondentSurname("Respondent Surname");
        filterDto.setRespondentPostcode("AA1 1AA");
        filterDto.setCourtCode("COURT1");
        filterDto.setOtherLocationDescription("Room 4");
        filterDto.setDate(LocalDate.of(2026, Month.JUNE, 4));
        filterDto.setCjaCode("CJA01");
        filterDto.setStatus(ApplicationListStatus.CLOSED);
        filterDto.setApplicationTitle("Application Title");

        var mappedResult = mapper.toApplicationListEntry(filterDto);

        Assertions.assertEquals(0L, mappedResult.getId());
        Assertions.assertEquals("ACC-456", mappedResult.getAccountNumber());
        Assertions.assertEquals("STD001", mappedResult.getStandardApplicant().getApplicantCode());
        Assertions.assertEquals("Applicant Org", mappedResult.getAnamedaddress().getName());
        Assertions.assertEquals("Applicant Surname", mappedResult.getAnamedaddress().getLastName());
        Assertions.assertEquals("Respondent Org", mappedResult.getRnameaddress().getName());
        Assertions.assertEquals("Respondent Surname", mappedResult.getRnameaddress().getLastName());
        Assertions.assertEquals("AA1 1AA", mappedResult.getRnameaddress().getPostcode());
        Assertions.assertEquals("COURT1", mappedResult.getApplicationList().getCourtCode());
        Assertions.assertEquals("Room 4", mappedResult.getApplicationList().getOtherLocation());
        Assertions.assertEquals(
                LocalDate.of(2026, Month.JUNE, 4), mappedResult.getApplicationList().getDate());
        Assertions.assertEquals("CJA01", mappedResult.getApplicationList().getCja().getCode());
        Assertions.assertEquals(Status.CLOSED, mappedResult.getApplicationList().getStatus());
    }

    @Test
    void testToApplicationListEntry_mapsPayloadForAuditEntryLookup() {
        val listId = UUID.randomUUID();
        val entryId = UUID.randomUUID();
        val payload = PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        var mappedResult = mapper.toApplicationListEntry(payload);

        Assertions.assertEquals(0L, mappedResult.getId());
        Assertions.assertEquals(listId, mappedResult.getApplicationList().getUuid());
        Assertions.assertEquals(entryId, mappedResult.getUuid());
    }

    @Test
    void testToApplicant_usesStandardApplicantWhenNamedApplicantMissing() {
        var projection = mock(ApplicationListEntryGetSummaryProjection.class);
        var standardApplicant = new StandardApplicantTestData().someComplete();
        var applicantMapper = new ApplicantMapperImpl();

        when(projection.getAnameAddress()).thenReturn(null);
        when(projection.getStandardApplicant()).thenReturn(standardApplicant);

        var expectedApplicant =
                applicantMapper.toApplicant(applicantMapper.toApplicantEntity(standardApplicant));

        var mappedApplicant = mapper.toApplicant(projection);

        assertThat(mappedApplicant).usingRecursiveComparison().isEqualTo(expectedApplicant);
    }

    @Test
    void testToApplicant_returnsNullWhenNoApplicantSourceExists() {
        var projection = mock(ApplicationListEntryGetSummaryProjection.class);

        when(projection.getAnameAddress()).thenReturn(null);
        when(projection.getStandardApplicant()).thenReturn(null);

        assertThat(mapper.toApplicant(projection)).isNull();
    }

    @Test
    void testMapOffsetDateTime_returnsLocalDateAndHandlesNull() {
        Assertions.assertNull(mapper.map((OffsetDateTime) null));
        Assertions.assertEquals(
                LocalDate.of(2026, Month.JUNE, 4),
                mapper.map(OffsetDateTime.of(2026, 6, 4, 9, 30, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    void testMapBooleanAndInteger_handleNullsAndValues() {
        Assertions.assertNull(mapper.map((Boolean) null));
        Assertions.assertEquals(YesOrNo.YES, mapper.map(Boolean.TRUE));
        Assertions.assertEquals(YesOrNo.NO, mapper.map(Boolean.FALSE));

        Assertions.assertNull(mapper.map((Integer) null));
        Assertions.assertEquals(Short.valueOf((short) 12), mapper.map(Integer.valueOf(12)));
    }

    @Test
    void testToStatus_mapsGeneratedStatusAndHandlesNull() {
        Assertions.assertNull(mapper.toStatus((ApplicationListStatus) null));
        Assertions.assertEquals(Status.CLOSED, mapper.toStatus(ApplicationListStatus.CLOSED));
    }

    @Test
    void testGetTemplateKeys_extractsWordingPlaceholders() {
        var applicationCode = new ApplicationCode();
        applicationCode.setWording("One {TEXT|First label|11} then {TEXT|Second label|11}");

        assertThat(mapper.getTemplateKeys(applicationCode))
                .containsExactly("First label", "Second label");
    }

    @Test
    void testToPrintDto_providePeople_validDtoGenerated() {
        var projection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicantTitle(MR)
                        .applicantLastName(PERSON4_SURNAME)
                        .applicantFirstName(PERSON4_FORENAME1)
                        .applicantMiddleName(
                                ApplicantMapper.combineMiddleName(
                                        PERSON4_FORENAME2, PERSON4_FORENAME3))
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
                        .respondentLastName(PERSON5_SURNAME)
                        .respondentFirstName(PERSON5_FORENAME1)
                        .respondentMiddleName(
                                ApplicantMapper.combineMiddleName(
                                        PERSON5_FORENAME2, PERSON5_FORENAME3))
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
        Assertions.assertEquals(PERSON4_FORENAME1, applicant.getName().getFirstName());
        Assertions.assertEquals(
                PERSON4_FORENAME2 + " " + PERSON4_FORENAME3,
                applicant.getName().getMiddleName().orElse(null));
        Assertions.assertEquals(PERSON4_SURNAME, applicant.getName().getLastName());
        Assertions.assertEquals(MRS, respondent.getName().getTitle());
        Assertions.assertEquals(PERSON5_FORENAME1, respondent.getName().getFirstName());
        Assertions.assertEquals(
                PERSON5_FORENAME2 + " " + PERSON5_FORENAME3,
                respondent.getName().getMiddleName().orElse(null));
        Assertions.assertEquals(PERSON5_SURNAME, respondent.getName().getLastName());
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
    void testToPrintDto_provideCanonicalisedStandardApplicantPerson_validDtoGenerated() {
        var projection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicantTitle(MR)
                        .applicantFirstName(PERSON4_FORENAME1)
                        .applicantMiddleName(
                                ApplicantMapper.combineMiddleName(
                                        PERSON4_FORENAME2, PERSON4_FORENAME3))
                        .applicantLastName(PERSON4_SURNAME)
                        .applicantAddressLine1(PERSON4_ADDRESSLINE1)
                        .applicantAddressLine2(PERSON4_ADDRESSLINE2)
                        .applicantAddressLine3(PERSON4_ADDRESSLINE3)
                        .applicantAddressLine4(PERSON4_ADDRESSLINE4)
                        .applicantAddressLine5(PERSON4_ADDRESSLINE5)
                        .applicantPostcode(PERSON4_POSTCODE)
                        .applicantPhone(PERSON4_PHONE)
                        .applicantMobile(PERSON4_MOBILE)
                        .applicantEmail(PERSON4_EMAIL)
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

        Assertions.assertEquals(
                PERSON4_FORENAME1, dto.getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                PERSON4_FORENAME2 + " " + PERSON4_FORENAME3,
                dto.getApplicant().getPerson().getName().getMiddleName().orElse(null));
        Assertions.assertEquals(
                PERSON4_SURNAME, dto.getApplicant().getPerson().getName().getLastName());
        Assertions.assertEquals(
                ORGANISATION2_NAME, dto.getRespondent().getOrganisation().getName());

        assertContactDetailsEqual(
                dto.getApplicant().getPerson().getContactDetails(),
                PERSON4_ADDRESSLINE1,
                PERSON4_ADDRESSLINE2,
                PERSON4_ADDRESSLINE3,
                PERSON4_ADDRESSLINE4,
                PERSON4_ADDRESSLINE5,
                PERSON4_POSTCODE,
                PERSON4_PHONE,
                PERSON4_MOBILE,
                PERSON4_EMAIL);

        assertApplicationDetailsEqual(dto);
    }

    @Test
    void
            testToPrintDto_whenApplicantAndRespondentTypesCannotBeDetermined_thenNoContactDetailsAreSet() {
        var projection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicationCode(APPLICATIONCODE1_CODE)
                        .applicationTitle(APPLICATIONCODE1_TITLE)
                        .applicationWording(APPLICATIONLISTENTRY1_WORDING)
                        .caseReference(APPLICATIONLISTENTRY1_CASEREFERENCE)
                        .accountReference(APPLICATIONLISTENTRY1_ACCOUNTNUMBER)
                        .notes(APPLICATIONLISTENTRY1_NOTES)
                        .build();

        var dto = new ApplicationListEntryMapperImpl().toPrintDto(projection);

        assertThat(dto.getApplicant()).isNotNull();
        if (dto.getApplicant().getPerson() != null) {
            assertThat(dto.getApplicant().getPerson().getContactDetails()).isNull();
        }
        if (dto.getApplicant().getOrganisation() != null) {
            assertThat(dto.getApplicant().getOrganisation().getContactDetails()).isNull();
        }
        assertThat(dto.getRespondent()).isNotNull();
        if (dto.getRespondent().getPerson() != null) {
            assertThat(dto.getRespondent().getPerson().getContactDetails()).isNull();
        }
        if (dto.getRespondent().getOrganisation() != null) {
            assertThat(dto.getRespondent().getOrganisation().getContactDetails()).isNull();
        }

        assertApplicationDetailsEqual(dto);
    }

    @Test
    void toEntrySummary() {
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
        respondent.setLastName("rsurname");
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
        respondent.setFirstName("rforename1");
        respondent.setMiddleName("rforename2 rforename3");

        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameAddress()).thenReturn(applicant);
        when(applicationListEntryGetSummaryProjection.getRnameAddress()).thenReturn(respondent);
        when(applicationListEntryGetSummaryProjection.getDateOfAl())
                .thenReturn(APPLICATION_LIST_DATE);

        when(applicationListEntryGetSummaryProjection.getAccountReference()).thenReturn("accref");
        when(applicationListEntryGetSummaryProjection.getCjaCode()).thenReturn("cjacode");
        when(applicationListEntryGetSummaryProjection.getCourtCode()).thenReturn("courtcode");
        when(applicationListEntryGetSummaryProjection.getLegislation()).thenReturn("leg");
        when(applicationListEntryGetSummaryProjection.getTitle()).thenReturn("title");

        when(applicationListEntryGetSummaryProjection.getRespondentSurname())
                .thenReturn("ressurname");
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.CLOSED);

        UUID uuidForProjection = UUID.randomUUID();
        when(applicationListEntryGetSummaryProjection.getUuid())
                .thenReturn(uuidForProjection.toString());

        UUID listId = UUID.randomUUID();
        when(applicationListEntryGetSummaryProjection.getListId()).thenReturn(listId.toString());

        when(applicationListEntryGetSummaryProjection.getDateOfAl())
                .thenReturn(APPLICATION_LIST_DATE);

        // run test
        EntryGetSummaryDto mappedResult =
                mapper.toEntrySummary(applicationListEntryGetSummaryProjection);

        assertEntrySummaryDetails(
                mappedResult,
                applicant,
                respondent,
                uuidForProjection,
                listId,
                APPLICATION_LIST_DATE);
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
                "Test template {TEXT|Applicant officer1|11} and second template "
                        + "{TEXT|Applicant officer2|11} and third template {TEXT|Applicant officer3|11}");

        appListEntry.setApplicationListEntryWording(
                "Test template {officerVal1} and second template "
                        + "{officerVal2} and third\" +\n"
                        + "                            \"template {officerVal3}");

        appListEntry.setApplicationCode(code);
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
        Fee offsite = feeTestData.someComplete();

        FeePair feePair = new FeePair(fee, offsite);

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setWordingTemplateMapper(new WordingTemplateMapper());

        EntryGetDetailDto entryGetDetailDto =
                mapper.toEntryGetDetailDto(
                        appListEntry,
                        List.of(applicationListStatus, applicationListStatus2),
                        feePair,
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

        validateApplicantPerson(applicant, entryGetDetailDto.getApplicant());
        validateRespondentOrganisation(respondent, entryGetDetailDto.getRespondent());
        assertWordingConstraints(
                entryGetDetailDto,
                List.of("Applicant officer1", "Applicant officer2", "Applicant officer3"),
                List.of("officerVal1", "officerVal2", "officerVal3"),
                List.of(11, 11, 11));
        assertOfficials(
                entryGetDetailDto.getOfficials(),
                List.of(appListEntryOfficial, appListEntryOfficial2));
        assertFeeStatuses(
                entryGetDetailDto.getFeeStatuses(),
                List.of(applicationListStatus, applicationListStatus2));
    }

    @Test
    void givenEntryFromClosedList_whenToEntryGetDetailDto_thenMapsContextCodeIdsAndNotes() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        NameAddress applicant = new NameAddressTestData().somePerson();
        ApplicationListEntry appListEntry = new AppListEntryTestData().someComplete();

        appListEntry.setUuid(entryId);
        appListEntry.getApplicationList().setUuid(listId);
        appListEntry.setAnamedaddress(applicant);
        appListEntry.setStandardApplicant(null);
        appListEntry.setNotes("Existing notes for closed update journey");
        appListEntry.setApplicationListEntryWording("Closed read wording");

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setCode("AD99002");
        applicationCode.setWording("Closed read wording");
        appListEntry.setApplicationCode(applicationCode);

        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setWordingTemplateMapper(new WordingTemplateMapper());

        EntryGetDetailDto dto = mapper.toEntryGetDetailDto(appListEntry, false);

        Assertions.assertEquals(entryId, dto.getId());
        Assertions.assertEquals(listId, dto.getListId());
        Assertions.assertEquals("AD99002", dto.getApplicationCode());
        Assertions.assertEquals("Existing notes for closed update journey", dto.getNotes());
        Assertions.assertFalse(dto.getHasOffsiteFee());
        validateApplicantPerson(applicant, dto.getApplicant());
    }

    @Test
    void givenLegacyOverlongStoredWording_whenToEntryGetDetailDto_thenReturnsStoredValue() {
        ApplicationListEntry appListEntry = new AppListEntryTestData().someComplete();
        appListEntry.setStandardApplicant(null);

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setWording("Legacy wording {TEXT|Reference|4}");
        appListEntry.setApplicationCode(applicationCode);
        appListEntry.setApplicationListEntryWording("Legacy wording {E1234567890}");

        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setWordingTemplateMapper(new WordingTemplateMapper());

        EntryGetDetailDto dto = mapper.toEntryGetDetailDto(appListEntry, false);

        Assertions.assertEquals("Legacy wording {{Reference}}", dto.getWording().getTemplate());
        Assertions.assertEquals(1, dto.getWording().getSubstitutionKeyConstraints().size());
        Assertions.assertEquals(
                "Reference", dto.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "E1234567890", dto.getWording().getSubstitutionKeyConstraints().get(0).getValue());
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

        appListEntry.setApplicationListEntryWording(
                "Test template {officer1} and second template "
                        + "{officer2} and third\" +\n"
                        + "                            \"template {officer3}");

        appListEntry.setApplicationCode(code);

        // execute the mapping
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setWordingTemplateMapper(new WordingTemplateMapper());

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

        assertWordingConstraints(
                entryGetDetailDto,
                List.of("Applicant officer1", "Applicant officer2", "Applicant officer3"),
                List.of("officer1", "officer2", "officer3"),
                List.of(10, 10, 10));
        assertOfficials(entryGetDetailDto.getOfficials(), appListEntry.getOfficials());
        assertFeeStatuses(entryGetDetailDto.getFeeStatuses(), appListEntry.getEntryFeeStatuses());
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
                        + "{TEXT|Applicant officer2|20} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|30}");

        appListEntry.setApplicationListEntryWording(
                "Test template {officeVal1} and second template "
                        + "{officeVal2} and third\" +\n"
                        + "                            \"template {officeVal3}");
        appListEntry.setApplicationCode(code);

        // execute the mapping
        mapper.setWordingTemplateMapper(new WordingTemplateMapper());
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

        assertWordingConstraints(
                entryGetDetailDto,
                List.of("Applicant officer1", "Applicant officer2", "Applicant officer3"),
                List.of("officeVal1", "officeVal2", "officeVal3"),
                List.of(10, 20, 30));
        assertOfficials(entryGetDetailDto.getOfficials(), appListEntry.getOfficials());
        assertFeeStatuses(entryGetDetailDto.getFeeStatuses(), appListEntry.getEntryFeeStatuses());
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

        appListEntry.setApplicationListEntryWording(
                "Test template {officeVal1} and second template "
                        + "{officeVal2} and third\" +\n"
                        + "                            \"template {officeVal3}");

        mapper.setWordingTemplateMapper(new WordingTemplateMapper());

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

        assertWordingConstraints(
                entryGetDetailDto,
                List.of("Applicant officer1", "Applicant officer2", "Applicant officer3"),
                List.of("officeVal1", "officeVal2", "officeVal3"),
                List.of(10, 10, 10));
        assertOfficials(entryGetDetailDto.getOfficials(), appListEntry.getOfficials());
        assertFeeStatuses(entryGetDetailDto.getFeeStatuses(), appListEntry.getEntryFeeStatuses());
    }

    private void assertEntrySummaryDetails(
            EntryGetSummaryDto mappedResult,
            NameAddress applicant,
            NameAddress respondent,
            UUID id,
            UUID listId,
            LocalDate date) {
        Assertions.assertEquals(ApplicationListStatus.CLOSED, mappedResult.getStatus());
        Assertions.assertEquals("leg", mappedResult.getLegislation());
        Assertions.assertEquals("title", mappedResult.getApplicationTitle());
        validateApplicantOrganisation(applicant, mappedResult.getApplicant());
        validateRespondentPerson(respondent, mappedResult.getRespondent());
        Assertions.assertFalse(mappedResult.getIsFeeRequired());
        Assertions.assertEquals(id.toString(), mappedResult.getId().toString());
        Assertions.assertEquals(listId.toString(), mappedResult.getListId().toString());
        Assertions.assertEquals(date, mappedResult.getDate());
        Assertions.assertEquals("accref", mappedResult.getAccountNumber().get());
    }

    private void assertWordingConstraints(
            EntryGetDetailDto entryGetDetailDto,
            List<String> expectedKeys,
            List<String> expectedValues,
            List<Integer> expectedLengths) {
        assertThat(entryGetDetailDto.getWording().getSubstitutionKeyConstraints())
                .hasSize(expectedKeys.size());

        for (int i = 0; i < expectedKeys.size(); i++) {
            var substitution =
                    entryGetDetailDto.getWording().getSubstitutionKeyConstraints().get(i);
            Assertions.assertEquals(expectedKeys.get(i), substitution.getKey());
            Assertions.assertEquals(expectedValues.get(i), substitution.getValue());
            Assertions.assertEquals(
                    expectedLengths.get(i), substitution.getConstraint().getLength());
            Assertions.assertEquals(
                    TemplateConstraint.TypeEnum.TEXT, substitution.getConstraint().getType());
        }
    }

    private void assertOfficials(
            List<Official> actualOfficials, List<AppListEntryOfficial> expectedOfficials) {
        assertThat(actualOfficials).isNotEmpty().hasSize(expectedOfficials.size());

        for (int i = 0; i < expectedOfficials.size(); i++) {
            var expectedOfficial = expectedOfficials.get(i);
            var actualOfficial = actualOfficials.get(i);
            Assertions.assertEquals(expectedOfficial.getSurname(), actualOfficial.getSurname());
            Assertions.assertEquals(expectedOfficial.getForename(), actualOfficial.getForename());
            Assertions.assertEquals(
                    officialMapper.toOfficial(expectedOfficial.getOfficialType()),
                    actualOfficial.getType());
            Assertions.assertEquals(expectedOfficial.getTitle(), actualOfficial.getTitle());
        }
    }

    private void assertFeeStatuses(
            List<FeeStatus> actualFeeStatuses, List<AppListEntryFeeStatus> expectedFeeStatuses) {
        assertThat(actualFeeStatuses).isNotEmpty().hasSize(expectedFeeStatuses.size());

        for (int i = 0; i < expectedFeeStatuses.size(); i++) {
            var expectedFeeStatus = expectedFeeStatuses.get(i);
            var actualFeeStatus = actualFeeStatuses.get(i);
            Assertions.assertEquals(
                    expectedFeeStatus.getAlefsPaymentReference(),
                    actualFeeStatus.getPaymentReference());
            Assertions.assertEquals(
                    mapper.getStatus(expectedFeeStatus.getAlefsFeeStatus()),
                    actualFeeStatus.getPaymentStatus());
        }
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
        Assertions.assertEquals(line2, actual.getAddressLine2().orElse(null));
        Assertions.assertEquals(line3, actual.getAddressLine3().orElse(null));
        Assertions.assertEquals(line4, actual.getAddressLine4().orElse(null));
        Assertions.assertEquals(line5, actual.getAddressLine5().orElse(null));
        Assertions.assertEquals(postcode, actual.getPostcode());
        Assertions.assertEquals(phone, actual.getPhone().orElse(null));
        Assertions.assertEquals(mobile, actual.getMobile().orElse(null));
        Assertions.assertEquals(email, actual.getEmail().orElse(null));
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
        Assertions.assertEquals(
                expectedLastName(entity), applicant.getPerson().getName().getLastName());
        Assertions.assertEquals(
                expectedFirstName(entity), applicant.getPerson().getName().getFirstName());
        Assertions.assertEquals(
                expectedMiddleName(entity),
                applicant.getPerson().getName().getMiddleName().orElse(null));
        Assertions.assertEquals(entity.getTitle(), applicant.getPerson().getName().getTitle());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                applicant.getPerson().getContactDetails().getMobile().orElse(null));
        Assertions.assertEquals(
                entity.getEmailAddress(),
                applicant.getPerson().getContactDetails().getEmail().orElse(null));
        Assertions.assertEquals(
                entity.getPostcode(), applicant.getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                applicant.getPerson().getContactDetails().getPhone().orElse(null));
        Assertions.assertEquals(
                entity.getAddress1(), applicant.getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                applicant.getPerson().getContactDetails().getAddressLine2().orElse(null));
        Assertions.assertEquals(
                entity.getAddress3(),
                applicant.getPerson().getContactDetails().getAddressLine3().orElse(null));
        Assertions.assertEquals(
                entity.getAddress4(),
                applicant.getPerson().getContactDetails().getAddressLine4().orElse(null));
        Assertions.assertEquals(
                entity.getAddress5(),
                applicant.getPerson().getContactDetails().getAddressLine5().orElse(null));
    }

    private void validateApplicantOrganisation(NameAddress entity, Applicant applicant) {
        // assert the applicant data
        Assertions.assertNotNull(applicant.getOrganisation());
        Assertions.assertEquals(entity.getName(), applicant.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                applicant.getOrganisation().getContactDetails().getMobile().orElse(null));
        Assertions.assertEquals(
                entity.getEmailAddress(),
                applicant.getOrganisation().getContactDetails().getEmail().orElse(null));
        Assertions.assertEquals(
                entity.getPostcode(),
                applicant.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                applicant.getOrganisation().getContactDetails().getPhone().orElse(null));
        Assertions.assertEquals(
                entity.getAddress1(),
                applicant.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                applicant.getOrganisation().getContactDetails().getAddressLine2().orElse(null));
        Assertions.assertEquals(
                entity.getAddress3(),
                applicant.getOrganisation().getContactDetails().getAddressLine3().orElse(null));
        Assertions.assertEquals(
                entity.getAddress4(),
                applicant.getOrganisation().getContactDetails().getAddressLine4().orElse(null));
        Assertions.assertEquals(
                entity.getAddress5(),
                applicant.getOrganisation().getContactDetails().getAddressLine5().orElse(null));
    }

    private void validateApplicantOrganisation(StandardApplicant entity, Applicant applicant) {
        // assert the applicant data
        Assertions.assertNotNull(applicant.getOrganisation());
        Assertions.assertEquals(entity.getName(), applicant.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                applicant.getOrganisation().getContactDetails().getMobile().orElse(null));
        Assertions.assertEquals(
                entity.getEmailAddress(),
                applicant.getOrganisation().getContactDetails().getEmail().orElse(null));
        Assertions.assertEquals(
                entity.getPostcode(),
                applicant.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                applicant.getOrganisation().getContactDetails().getPhone().orElse(null));
        Assertions.assertEquals(
                entity.getAddressLine1(),
                applicant.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddressLine2(),
                applicant.getOrganisation().getContactDetails().getAddressLine2().orElse(null));
        Assertions.assertEquals(
                entity.getAddressLine3(),
                applicant.getOrganisation().getContactDetails().getAddressLine3().orElse(null));
        Assertions.assertEquals(
                entity.getAddressLine4(),
                applicant.getOrganisation().getContactDetails().getAddressLine4().orElse(null));
        Assertions.assertEquals(
                entity.getAddressLine5(),
                applicant.getOrganisation().getContactDetails().getAddressLine5().orElse(null));
    }

    private void validateRespondentPerson(NameAddress entity, Respondent respondent) {
        // assert the applicant data
        Assertions.assertNotNull(respondent.getPerson());
        Assertions.assertEquals(
                expectedLastName(entity), respondent.getPerson().getName().getLastName());
        Assertions.assertEquals(
                expectedFirstName(entity), respondent.getPerson().getName().getFirstName());
        Assertions.assertEquals(
                expectedMiddleName(entity),
                respondent.getPerson().getName().getMiddleName().orElse(null));
        Assertions.assertEquals(entity.getTitle(), respondent.getPerson().getName().getTitle());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                respondent.getPerson().getContactDetails().getMobile().orElse(null));
        Assertions.assertEquals(
                entity.getEmailAddress(),
                respondent.getPerson().getContactDetails().getEmail().orElse(null));
        Assertions.assertEquals(
                entity.getPostcode(), respondent.getPerson().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                respondent.getPerson().getContactDetails().getPhone().orElse(null));
        Assertions.assertEquals(
                entity.getAddress1(), respondent.getPerson().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                respondent.getPerson().getContactDetails().getAddressLine2().orElse(null));
        Assertions.assertEquals(
                entity.getAddress3(),
                respondent.getPerson().getContactDetails().getAddressLine3().orElse(null));
        Assertions.assertEquals(
                entity.getAddress4(),
                respondent.getPerson().getContactDetails().getAddressLine4().orElse(null));
        Assertions.assertEquals(
                entity.getAddress5(),
                respondent.getPerson().getContactDetails().getAddressLine5().orElse(null));
        Assertions.assertEquals(entity.getDateOfBirth(), respondent.getPerson().getDateOfBirth());
    }

    private void validateRespondentOrganisation(NameAddress entity, Respondent respondent) {
        // assert the applicant data
        Assertions.assertNotNull(respondent.getOrganisation());
        Assertions.assertEquals(entity.getName(), respondent.getOrganisation().getName());
        Assertions.assertEquals(
                entity.getMobileNumber(),
                respondent.getOrganisation().getContactDetails().getMobile().orElse(null));
        Assertions.assertEquals(
                entity.getEmailAddress(),
                respondent.getOrganisation().getContactDetails().getEmail().orElse(null));
        Assertions.assertEquals(
                entity.getPostcode(),
                respondent.getOrganisation().getContactDetails().getPostcode());
        Assertions.assertEquals(
                entity.getTelephoneNumber(),
                respondent.getOrganisation().getContactDetails().getPhone().orElse(null));
        Assertions.assertEquals(
                entity.getAddress1(),
                respondent.getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertEquals(
                entity.getAddress2(),
                respondent.getOrganisation().getContactDetails().getAddressLine2().orElse(null));
        Assertions.assertEquals(
                entity.getAddress3(),
                respondent.getOrganisation().getContactDetails().getAddressLine3().orElse(null));
        Assertions.assertEquals(
                entity.getAddress4(),
                respondent.getOrganisation().getContactDetails().getAddressLine4().orElse(null));
        Assertions.assertEquals(
                entity.getAddress5(),
                respondent.getOrganisation().getContactDetails().getAddressLine5().orElse(null));
    }

    private String expectedFirstName(NameAddress entity) {
        return entity.getFirstName();
    }

    private String expectedMiddleName(NameAddress entity) {
        return entity.getMiddleName();
    }

    private String expectedLastName(NameAddress entity) {
        return entity.getLastName();
    }
}
