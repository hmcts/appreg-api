package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapperImpl;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ApplicantAssertion;

public class ApplicationEntryServiceImplTest extends BaseIntegration {

    @Autowired private ApplicationEntryService applicationEntryService;

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private FeeRepository feeRepository;

    @Autowired
    private ApplicationListEntryOfficialRepository applicationListEntryOfficialRepository;

    @Autowired private TransactionalUnitOfWork unitOfWork;

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
                                    applicationListRepository.findAll().getFirst();
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
                            applicationListRepository.findAll().getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    validateEntityAndResponseForEntryCreation(
                            entryCreateDto,
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

        // set the organisation and person applicant to null so we use the standard applicant
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.setNumberOfRespondents(0);

        // use the applicant standard applicant
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("CT99002");
        entryCreateDto.setWordingFields(List.of("test wording"));

        MatchResponse<EntryGetDetailDto> response;

        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository.findAll().getFirst();

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
                            applicationListRepository.findAll().getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    validateEntityAndResponseForEntryCreation(
                            entryCreateDto,
                            applicationListEntry,
                            response.getPayload(),
                            "Attends to swear a complaint for the issue of "
                                    + "a summons for the debtor to answer an application for a "
                                    + "liability order in relation to unpaid council tax (reference"
                                    + " test wording)",
                            List.of("Reference"));
                });
    }

    @Test
    public void createEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        final EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(10);

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of("test wording", LocalDate.now().toString()));

        MatchResponse<EntryGetDetailDto> response;

        response =
                unitOfWork.inTransaction(
                        () -> {
                            ApplicationList applicationList =
                                    applicationListRepository.findAll().getFirst();
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
                            applicationListRepository.findAll().getFirst();
                    List<ApplicationListEntry> entries =
                            applicationListEntryRepository.findByApplicationListId(
                                    applicationList.getId());

                    // gets the last added entry
                    ApplicationListEntry applicationListEntry = entries.getLast();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    validateEntityAndResponseForEntryCreation(
                            entryCreateDto,
                            applicationListEntry,
                            response.getPayload(),
                            "Application for a warrant to ente"
                                    + "r premises at test wording for date "
                                    + LocalDate.now(),
                            List.of("Premises Address", "Premises Date"));
                });
    }

    /**
     * validates the database and the restful output based on the input payload.
     *
     * @param entryCreateDto The entry create dto
     * @param applicationListEntry The application list database entity that was created
     * @param response The response from the service
     */
    private void validateEntityAndResponseForEntryCreation(
            EntryCreateDto entryCreateDto,
            ApplicationListEntry applicationListEntry,
            EntryGetDetailDto response,
            String assertWording,
            List<String> expectedWordingFields) {
        // validate applicant
        if (entryCreateDto.getStandardApplicantCode() != null) {
            // make sure the list is mapped to the code
            Assertions.assertEquals(
                    entryCreateDto.getStandardApplicantCode(),
                    applicationListEntry.getStandardApplicant().getApplicantCode());
        } else {
            if (entryCreateDto.getApplicant().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        entryCreateDto.getApplicant().getPerson(),
                        applicationListEntry.getAnamedaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        entryCreateDto.getApplicant().getOrganisation(),
                        applicationListEntry.getAnamedaddress());
            }
        }

        // validate respondent
        if (entryCreateDto.getRespondent() != null) {
            if (entryCreateDto.getRespondent().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        entryCreateDto.getRespondent().getPerson(),
                        applicationListEntry.getRnameaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        entryCreateDto.getRespondent().getOrganisation(),
                        applicationListEntry.getRnameaddress());
            }
            Assertions.assertEquals(
                    entryCreateDto.getRespondent().getDateOfBirth(),
                    applicationListEntry.getRnameaddress().getDateOfBirth());
        }

        // make sure the code of the applicant and respondent are set correctly
        if (entryCreateDto.getApplicant().getPerson() != null
                || entryCreateDto.getApplicant().getOrganisation() != null) {
            // validate the application code
            Assertions.assertEquals(
                    NameAddress.APPLICANT_CODE, applicationListEntry.getAnamedaddress().getCode());
        } else if (entryCreateDto.getRespondent().getPerson() != null
                || entryCreateDto.getRespondent().getOrganisation() != null) {
            // validate the application code
            Assertions.assertEquals(
                    NameAddress.RESPONDENT_CODE, applicationListEntry.getRnameaddress().getCode());
        }

        // if number or respondents is set make sure it was saved
        if (entryCreateDto.getNumberOfRespondents() != null) {
            Assertions.assertNull(applicationListEntry.getNumberOfBulkRespondents());
        }

        // assert offsite fee in the database is set if provided
        if (entryCreateDto.getFeeStatuses() != null && !entryCreateDto.getFeeStatuses().isEmpty()) {
            Assertions.assertEquals(
                    entryCreateDto.getHasOffsiteFee(),
                    feeRepository
                            .findById(applicationListEntry.getEntryFeeIds().get(0).getFeeId())
                            .get()
                            .isOffsite());
        }

        // make sure the core data is part of the entry
        Assertions.assertEquals(
                entryCreateDto.getCaseReference(), applicationListEntry.getCaseReference());
        Assertions.assertEquals(entryCreateDto.getNotes(), applicationListEntry.getNotes());
        Assertions.assertEquals(
                entryCreateDto.getAccountNumber(), applicationListEntry.getAccountNumber());
        Assertions.assertEquals(
                entryCreateDto.getLodgementDate(), applicationListEntry.getLodgementDate());
        Assertions.assertEquals(
                assertWording, applicationListEntry.getApplicationListEntryWording());

        // validate the fees are created in the database
        List<AppListEntryFeeStatus> fees =
                appListEntryFeeStatusRepository.findByAppListEntryId(applicationListEntry.getId());

        if (entryCreateDto.getFeeStatuses() != null) {
            for (int i = 0; i < entryCreateDto.getFeeStatuses().size(); i++) {
                Assertions.assertEquals(
                        entryCreateDto.getFeeStatuses().get(i).getPaymentReference(),
                        fees.get(i).getAlefsPaymentReference());
                Assertions.assertEquals(
                        ApplicationListEntryEntityMapper.toStatus(
                                entryCreateDto.getFeeStatuses().get(i).getPaymentStatus()),
                        fees.get(i).getAlefsFeeStatus());
            }
        }

        // validate the original are created
        List<AppListEntryOfficial> originals =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.getId());

        if (entryCreateDto.getOfficials() != null) {
            for (int i = 0; i < entryCreateDto.getOfficials().size(); i++) {
                Assertions.assertEquals(
                        entryCreateDto.getOfficials().get(i).getTitle(),
                        originals.get(i).getTitle());
                Assertions.assertEquals(
                        entryCreateDto.getOfficials().get(i).getForename(),
                        originals.get(i).getForename());
                Assertions.assertEquals(
                        entryCreateDto.getOfficials().get(i).getSurname(),
                        originals.get(i).getSurname());
                Assertions.assertEquals(
                        new OfficialMapperImpl()
                                .toOfficial(entryCreateDto.getOfficials().get(i).getType()),
                        originals.get(i).getOfficialType());
            }
        }

        // validate applicant in response
        if (entryCreateDto.getStandardApplicantCode() != null) {
            Applicant applicant =
                    new ApplicantMapperImpl()
                            .toApplicant(
                                    new ApplicantMapperImpl()
                                            .toApplicantEntity(
                                                    applicationListEntry.getStandardApplicant()));
            if (applicant.getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getApplicant().getPerson(),
                        applicationListEntry.getStandardApplicant());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getApplicant().getOrganisation(),
                        applicationListEntry.getStandardApplicant());
            }
        } else {
            if (entryCreateDto.getApplicant().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getApplicant().getPerson(),
                        applicationListEntry.getAnamedaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getApplicant().getOrganisation(),
                        applicationListEntry.getAnamedaddress());
            }
        }

        // validate respondent
        if (entryCreateDto.getRespondent() != null) {
            if (entryCreateDto.getRespondent().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getRespondent().getPerson(),
                        applicationListEntry.getRnameaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getRespondent().getOrganisation(),
                        applicationListEntry.getRnameaddress());
            }
            Assertions.assertEquals(
                    response.getRespondent().getDateOfBirth(),
                    applicationListEntry.getRnameaddress().getDateOfBirth());
        }

        // validate the response fields
        Assertions.assertEquals(entryCreateDto.getCaseReference(), response.getCaseReference());
        Assertions.assertEquals(entryCreateDto.getNotes(), response.getNotes());
        Assertions.assertEquals(entryCreateDto.getAccountNumber(), response.getAccountNumber());
        Assertions.assertEquals(expectedWordingFields, response.getWordingFields());
        Assertions.assertEquals(
                applicationListEntry.getApplicationList().getUuid(), response.getListId());
        Assertions.assertEquals(applicationListEntry.getUuid(), response.getId());
        Assertions.assertEquals(
                entryCreateDto.getNumberOfRespondents(), response.getNumberOfRespondents());
        Assertions.assertEquals(entryCreateDto.getLodgementDate(), response.getLodgementDate());

        Assertions.assertEquals(entryCreateDto.getHasOffsiteFee(), response.getHasOffsiteFee());

        // ensure the response fees align
        for (int i = 0; i < response.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getPaymentReference(),
                    response.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getStatusDate(),
                    response.getFeeStatuses().get(i).getStatusDate());
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getPaymentStatus(),
                    response.getFeeStatuses().get(i).getPaymentStatus());
        }

        // ensure the response fees align
        for (int i = 0; i < response.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getType(),
                    response.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getSurname(),
                    response.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getTitle(),
                    response.getOfficials().get(i).getTitle());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getForename(),
                    response.getOfficials().get(i).getForename());
        }
    }
}
