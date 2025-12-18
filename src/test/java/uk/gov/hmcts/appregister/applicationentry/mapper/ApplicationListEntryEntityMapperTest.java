package uk.gov.hmcts.appregister.applicationentry.mapper;

import java.time.LocalDate;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapperImpl;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;

class ApplicationListEntryEntityMapperTest {

    private ApplicationListEntryEntityMapper mapper;

    private ApplicantMapper applicantMapper;

    @BeforeEach
    void beforeEach() {
        mapper = new ApplicationListEntryEntityMapperImpl();
        applicantMapper = new ApplicantMapperImpl();
        mapper.setOfficialMapper(new OfficialMapperImpl());
    }

    @Test
    void testToApplicationListEntry() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        StandardApplicant standardApplicant =
                Instancio.of(StandardApplicant.class).withSettings(settings).create();

        NameAddress applicant = Instancio.of(NameAddress.class).withSettings(settings).create();

        NameAddress respondent = Instancio.of(NameAddress.class).withSettings(settings).create();

        ApplicationCode applicationCode =
                Instancio.of(ApplicationCode.class).withSettings(settings).create();

        ApplicationList applicationList =
                Instancio.of(ApplicationList.class).withSettings(settings).create();

        // perform the operation
        ApplicationListEntry applicationListEntry =
                mapper.toApplicationListEntry(
                        entryCreateDto,
                        "wording",
                        standardApplicant,
                        applicant,
                        respondent,
                        applicationCode,
                        applicationList);

        // make the assertion
        Assertions.assertEquals(standardApplicant, applicationListEntry.getStandardApplicant());
        Assertions.assertEquals(respondent, applicationListEntry.getRnameaddress());
        Assertions.assertEquals(applicant, applicationListEntry.getAnamedaddress());
        Assertions.assertEquals(applicationCode, applicationListEntry.getApplicationCode());
        Assertions.assertEquals(applicationList, applicationListEntry.getApplicationList());
        Assertions.assertEquals(
                entryCreateDto.getAccountNumber(), applicationListEntry.getAccountNumber());
        Assertions.assertEquals(
                entryCreateDto.getCaseReference(), applicationListEntry.getCaseReference());
        Assertions.assertEquals(entryCreateDto.getNotes(), applicationListEntry.getNotes());
        Assertions.assertEquals("wording", applicationListEntry.getApplicationListEntryWording());
    }

    @Test
    void testToFeeStatus() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationListEntry applicationList =
                Instancio.of(ApplicationListEntry.class).withSettings(settings).create();

        FeeStatus status = new FeeStatus();
        status.setStatusDate(LocalDate.now());
        status.setPaymentStatus(PaymentStatus.DUE);
        status.setPaymentReference("Ref");

        // perform the operation
        AppListEntryFeeStatus appListEntryFeeStatus = mapper.toFeeStatus(status, applicationList);

        // make the assertion
        Assertions.assertEquals(FeeStatusType.DUE, appListEntryFeeStatus.getAlefsFeeStatus());
        Assertions.assertEquals("Ref", appListEntryFeeStatus.getAlefsPaymentReference());
        Assertions.assertNotNull(appListEntryFeeStatus.getAlefsFeeStatusDate());
    }

    @Test
    void testToOfficial() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        ApplicationListEntry applicationList =
                Instancio.of(ApplicationListEntry.class).withSettings(settings).create();

        Official official = new Official();
        official.setType(OfficialType.MAGISTRATE);
        official.surname("surname");
        official.forename("forename");
        official.setTitle("title");

        AppListEntryOfficial appListEntryOfficial = mapper.toOfficial(official, applicationList);
        Assertions.assertEquals(
                uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE,
                appListEntryOfficial.getOfficialType());
        Assertions.assertEquals(official.getForename(), appListEntryOfficial.getForename());
        Assertions.assertEquals(official.getSurname(), appListEntryOfficial.getSurname());
        Assertions.assertEquals(official.getTitle(), appListEntryOfficial.getTitle());
    }

    @Test
    void testToApplicantPerson() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        Applicant applicant = Instancio.of(Applicant.class).withSettings(settings).create();
        applicant.setOrganisation(null);

        NameAddress nameAddress = applicantMapper.toApplicant(applicant);
        Assertions.assertEquals("AP", nameAddress.getCode());
        Assertions.assertEquals(
                applicant.getPerson().getName().getFirstForename(), nameAddress.getForename1());
        Assertions.assertEquals(
                applicant.getPerson().getName().getSecondForename(), nameAddress.getForename2());
        Assertions.assertEquals(
                applicant.getPerson().getName().getThirdForename(), nameAddress.getForename3());
        Assertions.assertEquals(
                applicant.getPerson().getName().getSurname(), nameAddress.getSurname());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getPhone(),
                nameAddress.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getEmail(),
                nameAddress.getEmailAddress());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getAddressLine1(),
                nameAddress.getAddress1());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getAddressLine2(),
                nameAddress.getAddress2());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getAddressLine3(),
                nameAddress.getAddress3());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getAddressLine4(),
                nameAddress.getAddress4());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getAddressLine5(),
                nameAddress.getAddress5());
        Assertions.assertEquals(
                applicant.getPerson().getContactDetails().getPostcode(), nameAddress.getPostcode());
    }

    @Test
    void testToApplicantOrganisation() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        Applicant applicant = Instancio.of(Applicant.class).withSettings(settings).create();
        applicant.setPerson(null);

        NameAddress nameAddress = applicantMapper.toApplicant(applicant);
        Assertions.assertEquals("AP", nameAddress.getCode());
        Assertions.assertEquals(nameAddress.getName(), applicant.getOrganisation().getName());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getPhone(),
                nameAddress.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getEmail(),
                nameAddress.getEmailAddress());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getAddressLine1(),
                nameAddress.getAddress1());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getAddressLine2(),
                nameAddress.getAddress2());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getAddressLine3(),
                nameAddress.getAddress3());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getAddressLine4(),
                nameAddress.getAddress4());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getAddressLine5(),
                nameAddress.getAddress5());
        Assertions.assertEquals(
                applicant.getOrganisation().getContactDetails().getPostcode(),
                nameAddress.getPostcode());
    }

    @Test
    void testToRespondentPerson() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        Respondent respondent = Instancio.of(Respondent.class).withSettings(settings).create();
        respondent.setOrganisation(null);

        NameAddress nameAddress = applicantMapper.toRespondent(respondent);
        Assertions.assertEquals("RE", nameAddress.getCode());
        Assertions.assertEquals(
                respondent.getPerson().getName().getFirstForename(), nameAddress.getForename1());
        Assertions.assertEquals(
                respondent.getPerson().getName().getSecondForename(), nameAddress.getForename2());
        Assertions.assertEquals(
                respondent.getPerson().getName().getThirdForename(), nameAddress.getForename3());
        Assertions.assertEquals(
                respondent.getPerson().getName().getSurname(), nameAddress.getSurname());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getPhone(),
                nameAddress.getTelephoneNumber());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getEmail(),
                nameAddress.getEmailAddress());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getAddressLine1(),
                nameAddress.getAddress1());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getAddressLine2(),
                nameAddress.getAddress2());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getAddressLine3(),
                nameAddress.getAddress3());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getAddressLine4(),
                nameAddress.getAddress4());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getAddressLine5(),
                nameAddress.getAddress5());
        Assertions.assertEquals(
                respondent.getPerson().getContactDetails().getPostcode(),
                nameAddress.getPostcode());
    }

    @Test
    void testToRespondentOrganisation() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        Respondent respondent = Instancio.of(Respondent.class).withSettings(settings).create();
        respondent.setPerson(null);

        NameAddress nameAddress = applicantMapper.toRespondent(respondent);
        Assertions.assertEquals("RE", nameAddress.getCode());
        Assertions.assertEquals(nameAddress.getName(), respondent.getOrganisation().getName());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getPhone(),
                nameAddress.getTelephoneNumber());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getEmail(),
                nameAddress.getEmailAddress());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getAddressLine1(),
                nameAddress.getAddress1());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getAddressLine2(),
                nameAddress.getAddress2());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getAddressLine3(),
                nameAddress.getAddress3());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getAddressLine4(),
                nameAddress.getAddress4());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getAddressLine5(),
                nameAddress.getAddress5());
        Assertions.assertEquals(
                respondent.getOrganisation().getContactDetails().getPostcode(),
                nameAddress.getPostcode());
    }
}
