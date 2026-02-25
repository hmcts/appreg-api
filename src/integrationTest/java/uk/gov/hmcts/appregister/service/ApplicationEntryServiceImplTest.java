package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryAssertion;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryWrapperDto;

@Slf4j
public class ApplicationEntryServiceImplTest extends BaseIntegration {

    @Autowired private ApplicationEntryService applicationEntryService;

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private AppListEntryFeeRepository appListEntryFeeRepository;

    @Autowired private FeeRepository feeRepository;

    @Autowired
    private ApplicationListEntryOfficialRepository applicationListEntryOfficialRepository;

    @Autowired private NameAddressRepository nameAddressRepository;

    @Autowired private TransactionalUnitOfWork unitOfWork;

    @Autowired private EntityManager entityManager;

    @Autowired private ApplicationListEntryAssertion applicationListEntryAssertion;

    @BeforeEach
    public void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void createEntryNoRespondentWithOffsiteFee() {

        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(true);

        MatchResponse<EntryGetDetailDto> response;

        // run the test
        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository
                                            .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                            .getFirst();

                            // because of the random order of tests, this can fail so need to
                            // make sure the application list is in a valid state
                            applicationList.setStatus(Status.OPEN);
                            applicationList.setDeleted(false);
                            applicationListRepository.save(applicationList);
                            applicationListRepository.flush();

                            PayloadForCreate<EntryCreateDto> payloadForCreate =
                                    PayloadForCreate.<EntryCreateDto>builder()
                                            .id(applicationList.getUuid())
                                            .data(entryCreateDto)
                                            .build();
                            return applicationEntryService.createEntry(payloadForCreate);
                        });

        // make the assertions
        unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request to copy documents",
                            List.of());
                });
    }

    @Test
    public void createEntryWithRespondentWithoutFeeDueNoBulkRespondent() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Reference");
        substitution.setValue("test wording");

        // set the organisation and person applicant to null so we use the standard applicant
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.getRespondent().setOrganisation(null);
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
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.setNumberOfRespondents(0);
        entryCreateDto.setWordingFields(List.of(substitution));

        // use the applicant standard applicant
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("CT99002");
        entryCreateDto.setWordingFields(List.of(substitution));

        MatchResponse<EntryGetDetailDto> response;

        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository
                                            .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                            .getFirst();

                            PayloadForCreate<EntryCreateDto> payloadForCreate =
                                    PayloadForCreate.<EntryCreateDto>builder()
                                            .id(applicationList.getUuid())
                                            .data(entryCreateDto)
                                            .build();
                            return applicationEntryService.createEntry(payloadForCreate);
                        });

        // make the assertions
        unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Attends to swear a complaint for the issue of "
                                    + "a summons for the debtor to answer an application for a "
                                    + "liability order in relation to unpaid council tax (reference"
                                    + " {test wording})",
                            List.of("Reference"));
                });
    }

    @Test
    public void createEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
        createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest();
    }

    @Test
    @Transactional
    public void updateEntryNoRespondentWithOffsiteFee() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // build the payload
        EntryUpdateDto entryUpdateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setApplicationCode("AD99001");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setWordingFields(null);
        entryUpdateDto.setHasOffsiteFee(true);

        UUID uuid = createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        List<AppListEntryOfficial> feeOfficial =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        entryUpdateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());

        // get the existing applicant and respondent for later comparison
        NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        // get the ids of the status and officials
        final List<Long> feeStatusBeforeUpdate =
                feeStatuses.stream().map(fs -> fs.getId()).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(fs -> fs.getId()).toList();

        MatchResponse<EntryGetDetailDto> update =
                applicationEntryService.updateEntry(payloadForCreate);

        // assert that the update was successful
        Assertions.assertNotNull(update.getEtag());

        final List<AppListEntryFeeStatus> feeStatusesUpdated =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        final List<AppListEntryOfficial> feeOfficialUpdated =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // assert that old name does not exist
        Assertions.assertNotNull(respondentBeforeUpdate);
        Assertions.assertNotNull(applicantBeforeUpdate);

        entityManager.clear();

        Assertions.assertTrue(
                nameAddressRepository.findById(respondentBeforeUpdate.getId()).isEmpty());
        Assertions.assertTrue(
                nameAddressRepository.findById(applicantBeforeUpdate.getId()).isEmpty());

        // make sure we do not recognise the officials that existing before
        Assertions.assertEquals(
                update.getPayload().getOfficials().size(), feeOfficialUpdated.size());
        for (Long id : feeOfficialBeforeUpdate) {
            Assertions.assertFalse(
                    feeOfficialUpdated.stream().anyMatch(fo -> fo.getId().equals(id)),
                    "Found official with id " + id + " that should have been deleted");
        }

        // make sure we have preserved the old status fees
        Assertions.assertEquals(
                update.getPayload().getFeeStatuses().size(),
                (long) entryUpdateDto.getFeeStatuses().size()
                        + (long) feeStatusBeforeUpdate.size());
        for (Long id : feeStatusBeforeUpdate) {
            Assertions.assertTrue(
                    feeStatusesUpdated.stream().anyMatch(fs -> fs.getId().equals(id)),
                    "Did not find fee status with id " + id + " that should have been preserved");
        }

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Request to copy documents",
                List.of(),
                feeStatusBeforeUpdate);
    }

    @Test
    @Transactional
    public void updateEntryWithOffsiteFeeAndStandardApplicant() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        UUID uuid = createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        List<AppListEntryOfficial> feeOfficial =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // get the ids of the status and officials
        final List<Long> feeStatusBeforeUpdate =
                feeStatuses.stream().map(fs -> fs.getId()).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(fs -> fs.getId()).toList();

        // get the existing applicant and respondent for later comparison
        final NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        final NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        // build the payload
        EntryUpdateDto entryUpdateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setApplicant(null);
        entryUpdateDto.setStandardApplicantCode("APP001");
        entryUpdateDto.setApplicationCode("ZS99007");

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        entryUpdateDto.setWordingFields(List.of(substitution, substitution1));
        entryUpdateDto.setHasOffsiteFee(true);
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of(null));
        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        entryUpdateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());
        MatchResponse<EntryGetDetailDto> update =
                applicationEntryService.updateEntry(payloadForCreate);

        entityManager.clear();

        // assert that the update was successful
        Assertions.assertNotNull(update.getEtag());

        final List<AppListEntryFeeStatus> feeStatusesUpdated =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        final List<AppListEntryOfficial> feeOfficialUpdated =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // assert that old name does not exist
        Assertions.assertNotNull(respondentBeforeUpdate);
        Assertions.assertNotNull(applicantBeforeUpdate);

        Assertions.assertTrue(
                nameAddressRepository.findById(respondentBeforeUpdate.getId()).isEmpty());
        Assertions.assertTrue(
                nameAddressRepository.findById(applicantBeforeUpdate.getId()).isEmpty());

        // make sure the fee is mapped correctly to the entry
        List<Fee> fees =
                appListEntryFeeRepository.getFeeForEntryId(applicationListEntry.get().getId());
        Assertions.assertEquals(1, fees.size());
        Assertions.assertEquals(
                "Application to state a case for the High Court", fees.get(0).getDescription());

        // make sure we do not recognise the officials that existing before
        Assertions.assertEquals(
                update.getPayload().getOfficials().size(), feeOfficialUpdated.size());
        for (Long id : feeOfficialBeforeUpdate) {
            Assertions.assertFalse(
                    feeOfficialUpdated.stream().anyMatch(fo -> fo.getId().equals(id)),
                    "Found official with id " + id + " that should have been deleted");
        }

        // make sure we have preserved the old status fees
        Assertions.assertEquals(
                update.getPayload().getFeeStatuses().size(),
                (long) entryUpdateDto.getFeeStatuses().size()
                        + (long) feeStatusBeforeUpdate.size());
        for (Long id : feeStatusBeforeUpdate) {
            Assertions.assertTrue(
                    feeStatusesUpdated.stream().anyMatch(fs -> fs.getId().equals(id)),
                    "Did not find fee status with id " + id + " that should have been preserved");
        }

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);

        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Application for a warrant to enter premises at {value} for date {"
                        + LocalDate.now()
                        + "}",
                List.of("Premises Address", "Premises Date"),
                feeStatusBeforeUpdate);
    }

    @Test
    @Transactional
    public void updateEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        UUID uuid = createEntryNoRespondentWithOffsiteFeeForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        List<AppListEntryOfficial> feeOfficial =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // get the ids of the status and officials
        final List<Long> feeStatusBeforeUpdate =
                feeStatuses.stream().map(fs -> fs.getId()).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(fs -> fs.getId()).toList();

        // get the existing applicant and respondent for later comparison
        final NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        final NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        final EntryUpdateDto updateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();
        updateDto.getApplicant().setOrganisation(null);
        updateDto.getApplicant().getPerson().getName().setSecondForename(JsonNullable.of(null));
        updateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        updateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        updateDto.getRespondent().setOrganisation(null);
        updateDto.getRespondent().getPerson().getName().setSecondForename(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getName().setThirdForename(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        updateDto.setNumberOfRespondents(null);
        updateDto.setApplicationCode("MS99007");
        updateDto.setStandardApplicantCode(null);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        updateDto.setWordingFields(List.of(substitution, substitution1));

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        updateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());
        MatchResponse<EntryGetDetailDto> update =
                applicationEntryService.updateEntry(payloadForCreate);

        entityManager.clear();

        // assert that the update was successful
        Assertions.assertNotNull(update.getEtag());

        final List<AppListEntryFeeStatus> feeStatusesUpdated =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        final List<AppListEntryOfficial> feeOfficialUpdated =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // assert that old name does not exist
        Assertions.assertNull(respondentBeforeUpdate);
        Assertions.assertNotNull(applicantBeforeUpdate);

        Assertions.assertTrue(
                nameAddressRepository.findById(applicantBeforeUpdate.getId()).isEmpty());

        // make sure we do not recognise the officials that existing before
        Assertions.assertEquals(
                update.getPayload().getOfficials().size(), feeOfficialUpdated.size());
        for (Long id : feeOfficialBeforeUpdate) {
            Assertions.assertFalse(
                    feeOfficialUpdated.stream().anyMatch(fo -> fo.getId().equals(id)),
                    "Found official with id " + id + " that should have been deleted");
        }

        // make sure we have preserved the old status fees
        Assertions.assertEquals(
                update.getPayload().getFeeStatuses().size(),
                (long) updateDto.getFeeStatuses().size() + (long) feeStatusBeforeUpdate.size());
        for (Long id : feeStatusBeforeUpdate) {
            Assertions.assertTrue(
                    feeStatusesUpdated.stream().anyMatch(fs -> fs.getId().equals(id)),
                    "Did not find fee status with id " + id + " that should have been preserved");
        }

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(updateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Application for a warrant to enter"
                        + " premises at {value} for date {"
                        + LocalDate.now()
                        + "}",
                List.of("Premises Address", "Premises Date"),
                feeStatusBeforeUpdate);
    }

    @Test
    @Transactional
    public void updateEntryWithRespondentWithoutFeeDueNoBulkRespondent() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        UUID uuid = createEntryNoRespondentWithOffsiteFeeForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        List<AppListEntryOfficial> feeOfficial =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // get the ids of the status and officials
        final List<Long> feeStatusBeforeUpdate =
                feeStatuses.stream().map(fs -> fs.getId()).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(fs -> fs.getId()).toList();

        // get the existing applicant and respondent for later comparison
        final NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        final NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        final EntryUpdateDto updateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();
        // set the organisation and person applicant to null so we use the standard applicant
        updateDto.getApplicant().setOrganisation(null);
        updateDto.getApplicant().setPerson(null);
        updateDto.setFeeStatuses(null);
        updateDto.getRespondent().setOrganisation(null);
        updateDto.getRespondent().getPerson().getName().setThirdForename(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getName().setSecondForename(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        updateDto.setNumberOfRespondents(0);

        // use the applicant standard applicant
        updateDto.setStandardApplicantCode("APP001");
        updateDto.setNumberOfRespondents(null);
        updateDto.setApplicationCode("CT99002");

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Reference");
        substitution.setValue("test wording");

        updateDto.setWordingFields(List.of(substitution));

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        updateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());
        MatchResponse<EntryGetDetailDto> update =
                applicationEntryService.updateEntry(payloadForCreate);

        entityManager.clear();

        // assert that the update was successful
        Assertions.assertNotNull(update.getEtag());

        final List<AppListEntryFeeStatus> feeStatusesUpdated =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        final List<AppListEntryOfficial> feeOfficialUpdated =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // assert that old name does not exist
        Assertions.assertNull(respondentBeforeUpdate);
        Assertions.assertNotNull(applicantBeforeUpdate);

        Assertions.assertTrue(
                nameAddressRepository.findById(applicantBeforeUpdate.getId()).isEmpty());

        // make sure we do not recognise the officials that existing before
        Assertions.assertEquals(
                update.getPayload().getOfficials().size(), feeOfficialUpdated.size());
        for (Long id : feeOfficialBeforeUpdate) {
            Assertions.assertFalse(
                    feeOfficialUpdated.stream().anyMatch(fo -> fo.getId().equals(id)),
                    "Found official with id " + id + " that should have been deleted");
        }

        // make sure we have preserved the old status fees
        Assertions.assertEquals(
                update.getPayload().getFeeStatuses().size(),
                updateDto.getFeeStatuses() != null
                        ? updateDto.getFeeStatuses().size()
                        : (long) feeStatusBeforeUpdate.size());

        for (Long id : feeStatusBeforeUpdate) {
            Assertions.assertTrue(
                    feeStatusesUpdated.stream().anyMatch(fs -> fs.getId().equals(id)),
                    "Did not find fee status with id " + id + " that should have been preserved");
        }

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(updateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Attends to swear a complaint for the issue of a summons for the "
                        + "debtor to answer an application for a liability order in relation "
                        + "to unpaid council tax (reference {test wording})",
                List.of("Reference"),
                feeStatusBeforeUpdate);
    }

    // useful method to create an entry with respondent, bulk respondent and fee statuses for update
    // purposes

    /**
     * Creates an entry and returns the UUID.
     *
     * @return The UUID of the created entry
     */
    private UUID createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        final EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.getApplicant().setOrganisation(null);

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
                .setAddressLine2(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(Instancio.gen().string().get()));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto.getRespondent().setOrganisation(null);

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
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));

        entryCreateDto.setNumberOfRespondents(10);

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

        MatchResponse<EntryGetDetailDto> response;

        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository
                                            .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                            .get(4);
                            PayloadForCreate<EntryCreateDto> payloadForCreate =
                                    PayloadForCreate.<EntryCreateDto>builder()
                                            .id(applicationList.getUuid())
                                            .data(entryCreateDto)
                                            .build();
                            return applicationEntryService.createEntry(payloadForCreate);
                        });

        // make the assertions
        unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .get(4);
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Application for a warrant to ente"
                                    + "r premises at {test wording} for date {"
                                    + LocalDate.now()
                                    + "}",
                            List.of("Premises Address", "Premises Date"));
                });

        return response.getPayload().getId();
    }

    public UUID createEntryNoRespondentWithOffsiteFeeForTest() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(true);

        MatchResponse<EntryGetDetailDto> response;

        // run the test
        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository
                                            .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                            .getFirst();
                            PayloadForCreate<EntryCreateDto> payloadForCreate =
                                    PayloadForCreate.<EntryCreateDto>builder()
                                            .id(applicationList.getUuid())
                                            .data(entryCreateDto)
                                            .build();
                            return applicationEntryService.createEntry(payloadForCreate);
                        });

        // make the assertions
        unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request to copy documents",
                            List.of());
                });
        return response.getPayload().getId();
    }
}
