package uk.gov.hmcts.appregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.text.ParseException;
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
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
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
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryAssertion;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryWrapperDto;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

@Slf4j
class ApplicationEntryServiceImplTest extends BaseIntegration {

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
    void setUp() throws JOSEException, ParseException {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createEntryNoRespondentWithOffsiteFeeLodgementDateInThePast() {

        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setLodgementDate(LocalDate.parse("2016-01-01"));
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .orElseThrow();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request to copy documents",
                            "Request to copy documents",
                            List.of(),
                            1);

                    Fee offsiteFee = null;
                    for (AppListEntryFeeId entryFeeId : applicationListEntry.getEntryFeeIds()) {
                        Optional<Fee> fee = feeRepository.findById(entryFeeId.getFeeId());
                        if (fee.isPresent() && fee.get().isOffsite()) {
                            offsiteFee = fee.get();
                            break;
                        }
                    }

                    assertThat(offsiteFee).isNotNull();

                    // check the fee record for the offsite fee
                    assertThat(offsiteFee.getStartDate())
                            .isBeforeOrEqualTo(applicationListEntry.getLodgementDate());
                    if (offsiteFee.getEndDate() != null) {
                        assertThat(offsiteFee.getEndDate())
                                .isAfterOrEqualTo(applicationListEntry.getLodgementDate());
                    }
                    assertThat(offsiteFee.getAmount()).isEqualTo(new BigDecimal("70.00"));
                });
    }

    @Test
    void createEntryNoRespondentWithOffsiteFeeLodgementDateToday() {

        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .orElseThrow();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request to copy documents",
                            "Request to copy documents",
                            List.of(),
                            2);

                    Fee offsiteFee = null;
                    for (AppListEntryFeeId entryFeeId : applicationListEntry.getEntryFeeIds()) {
                        Optional<Fee> fee = feeRepository.findById(entryFeeId.getFeeId());
                        if (fee.isPresent() && fee.get().isOffsite()) {
                            offsiteFee = fee.get();
                            break;
                        }
                    }

                    assertThat(offsiteFee).isNotNull();

                    // check the fee record for the offsite fee
                    assertThat(offsiteFee.getStartDate())
                            .isBeforeOrEqualTo(applicationListEntry.getLodgementDate());
                    if (offsiteFee.getEndDate() != null) {
                        assertThat(offsiteFee.getEndDate())
                                .isAfterOrEqualTo(applicationListEntry.getLodgementDate());
                    }
                    assertThat(offsiteFee.getAmount()).isEqualTo(new BigDecimal("29.00"));
                });
    }

    @Test
    void createEntryNoRespondentWithFeeLodgementDateInThePast() {

        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.setLodgementDate(LocalDate.parse("2015-01-01"));

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(false);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .orElseThrow();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    Assertions.assertEquals("AD99001", response.getPayload().getApplicationCode());
                    Assertions.assertEquals(
                            "Request to copy documents",
                            applicationListEntry.getApplicationListEntryWording());
                    Assertions.assertTrue(applicationListEntry.getEntryFeeIds().isEmpty());
                });
    }

    @Test
    void createEntryNoRespondentWithFeeLodgementDateToday() {

        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));

        entryCreateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(false);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                            "Request to copy documents",
                            List.of(),
                            1);

                    Assertions.assertEquals(1, applicationListEntry.getEntryFeeIds().size());

                    Optional<Fee> fee =
                            feeRepository.findById(
                                    applicationListEntry.getEntryFeeIds().getFirst().getFeeId());
                    Assertions.assertTrue(fee.isPresent());
                    assertThat(fee.get().getStartDate())
                            .isBeforeOrEqualTo(applicationListEntry.getLodgementDate());

                    // to make sure it's the current fee and not a historic fee
                    Assertions.assertNull(fee.get().getEndDate());
                    assertThat(fee.get().getAmount()).isEqualTo(new BigDecimal("11.00"));
                });
    }

    @Test
    void createEntryWithRespondentWithoutFeeDueNoBulkRespondent() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));

        // set the organisation and person applicant to null so we use the standard applicant
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.setNumberOfRespondents(0);

        // use the applicant standard applicant
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MX99010");
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Summarise offence title(s)");
        substitution.setValue("test wording");

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
                            "Application for the issue of a summons on an information laid in a private "
                                    + "prosecution alleging {test wording}",
                            "Application for the issue of a summons on an information laid in a private "
                                    + "prosecution alleging {{Summarise offence title(s)}}",
                            List.of(substitution),
                            2);
                });
    }

    @Test
    void createEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
        createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest();
    }

    @Test
    void createEntryWithCodeFeeReferencingOffsiteFeeExpectSingleFeeRecord() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setOfficials(limitOfficials(entryCreateDto.getOfficials()));
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.getRespondent().setOrganisation(null);

        FullName name = new FullName();
        name.setTitle("Mr");
        name.setFirstName("John");
        name.setMiddleName(JsonNullable.of(null));
        name.setMiddleName(JsonNullable.of(null));
        name.setLastName("Smith");

        entryCreateDto.getRespondent().getPerson().setName(name);

        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.getRespondent().getPerson().getContactDetails().setAddressLine1("line1");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("01234567890"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@test.com"));

        // no respondent for this code
        entryCreateDto.setApplicationCode("AD99002");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
        // make the assertions
        unitOfWork.inTransaction(
                () -> {
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .orElseThrow();

                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request for copy documents on computer disc or in electronic form",
                            "Request for copy documents on computer disc or in electronic form",
                            List.of(),
                            2);
                });
    }

    @Test
    void createEntryWithCodeFeeNotReferencingOffsiteFeeButOffsiteFeeAttachedExpectTwoFeeRecords() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setOfficials(limitOfficials(entryCreateDto.getOfficials()));
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.getRespondent().setOrganisation(null);

        FullName name = new FullName();
        name.setTitle("Mr");
        name.setFirstName("John");
        name.setMiddleName(JsonNullable.of(null));
        name.setMiddleName(JsonNullable.of(null));
        name.setLastName("Smith");

        entryCreateDto.getRespondent().getPerson().setName(name);

        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.getRespondent().getPerson().getContactDetails().setAddressLine1("line1");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("01234567890"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@test.com"));

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99002");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setHasOffsiteFee(true);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        entryCreateDto.setWordingFields(null);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .orElseThrow();

                    // validate the database based on the request data and the response
                    // based on the database contents
                    applicationListEntryAssertion.validateEntityAndResponseForEntryCreation(
                            new ApplicationListEntryWrapperDto(entryCreateDto),
                            applicationListEntry,
                            response.getPayload(),
                            "Request for copy documents on computer disc or in electronic form",
                            "Request for copy documents on computer disc or in electronic form",
                            List.of(),
                            2);
                });
    }

    @Test
    @Transactional
    void updateEntryNoRespondentWithOffsiteFeeLodgementDateToday() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // build the payload
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setApplicationCode("AD99001");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setWordingFields(null);
        entryUpdateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

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

        // get the ids of the fee statuses
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(Keyable::getId).toList();

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

        Assertions.assertEquals(
                entryUpdateDto.getFeeStatuses().size(),
                update.getPayload().getFeeStatuses().size());
        Assertions.assertEquals(entryUpdateDto.getFeeStatuses().size(), feeStatusesUpdated.size());

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Request to copy documents",
                "Request to copy documents",
                List.of(),
                List.of(),
                2);
    }

    @Test
    @Transactional
    void updateEntryNoRespondentWithOffsiteFeeLodgementDateInThePast() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // build the payload
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setApplicationCode("AD99001");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setWordingFields(null);
        entryUpdateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

        UUID uuid =
                createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest(
                        LocalDate.parse("2016-01-01"));

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
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(Keyable::getId).toList();

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

        Assertions.assertEquals(
                entryUpdateDto.getFeeStatuses().size(),
                update.getPayload().getFeeStatuses().size());
        Assertions.assertEquals(entryUpdateDto.getFeeStatuses().size(), feeStatusesUpdated.size());

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto, LocalDate.parse("2016-01-01")),
                applicationListEntry.get(),
                update.getPayload(),
                "Request to copy documents",
                "Request to copy documents",
                List.of(),
                List.of(),
                1);

        // validate that the fee records have not changed except for the offsite fee being added
        for (AppListEntryFeeId entryFeeID : applicationListEntry.get().getEntryFeeIds()) {
            Optional<Fee> fee = feeRepository.findById(entryFeeID.getFeeId());
            Assertions.assertTrue(fee.isPresent());
            assertThat(applicationListEntry.get().getLodgementDate())
                    .isAfterOrEqualTo(fee.get().getStartDate());
            assertThat(applicationListEntry.get().getLodgementDate())
                    .isBeforeOrEqualTo(fee.get().getEndDate());

            if (fee.get().isOffsite()) {
                assertThat(fee.get().getAmount()).isEqualTo(new BigDecimal("70.00"));
            } else {
                assertThat(fee.get().getAmount()).isEqualTo(new BigDecimal("50.00"));
            }
        }
    }

    @Test
    @Transactional
    void updateEntryWithOffsiteFeeAndStandardApplicant() {
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
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(AppListEntryOfficial::getId).toList();

        // get the existing applicant and respondent for later comparison
        final NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        final NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        // build the payload
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setApplicant(null);
        entryUpdateDto.setStandardApplicantCode("APP001");
        entryUpdateDto.setApplicationCode("ZS99007");

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        entryUpdateDto.setWordingFields(List.of(substitution, substitution1));
        entryUpdateDto.setHasOffsiteFee(true);
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        entryUpdateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());
        MatchResponse<EntryGetDetailDto> update =
                applicationEntryService.updateEntry(payloadForCreate);

        entityManager.clear();

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
        Assertions.assertEquals(2, fees.size());
        Assertions.assertTrue(
                fees.stream()
                        .anyMatch(
                                fee ->
                                        fee.getDescription()
                                                .equals(
                                                        "Application to state a case for the High Court")));
        Assertions.assertTrue(fees.stream().anyMatch(Fee::isOffsite));

        // make sure we do not recognise the officials that existing before
        Assertions.assertEquals(
                update.getPayload().getOfficials().size(), feeOfficialUpdated.size());
        for (Long id : feeOfficialBeforeUpdate) {
            Assertions.assertFalse(
                    feeOfficialUpdated.stream().anyMatch(fo -> fo.getId().equals(id)),
                    "Found official with id " + id + " that should have been deleted");
        }

        Assertions.assertEquals(entryUpdateDto.getFeeStatuses().size(), feeStatusesUpdated.size());

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);

        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Application for a warrant to enter premises at {value} for date {"
                        + LocalDate.now(java.time.ZoneOffset.UTC)
                        + "}",
                "Application for a warrant to enter premises at {{Premises Address}} "
                        + "for date {{Premises Date}}",
                entryUpdateDto.getWordingFields(),
                List.of(),
                2);
    }

    @Test
    @Transactional
    void updateEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
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
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(Keyable::getId).toList();

        // get the existing applicant and respondent for later comparison
        final NameAddress respondentBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getRnameaddress());
        final NameAddress applicantBeforeUpdate =
                BeanUtil.copyBean(applicationListEntry.get().getAnamedaddress());

        final EntryUpdateDto updateDto = createEntryUpdateDto(settings);
        updateDto.getApplicant().setOrganisation(null);
        updateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        updateDto.getRespondent().setOrganisation(null);
        updateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        updateDto.setHasOffsiteFee(true);
        updateDto.setNumberOfRespondents(null);
        updateDto.setApplicationCode("MS99007");
        updateDto.setStandardApplicantCode(null);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        // fill the template with the two parameters
        updateDto.setWordingFields(List.of(substitution, substitution1));

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(updateDto.getFeeStatuses());

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

        Assertions.assertEquals(updateDto.getFeeStatuses().size(), feeStatusesUpdated.size());

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(updateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Application for a warrant to enter"
                        + " premises at {value} for date {"
                        + LocalDate.now(java.time.ZoneOffset.UTC)
                        + "}",
                "Application for a warrant to enter premises at "
                        + "{{Premises Address}} for date {{Premises Date}}",
                List.of(updateDto.getWordingFields().toArray(new TemplateSubstitution[0])),
                List.of(),
                1);
    }

    @Test
    @Transactional
    void updateEntryWithRespondentWithoutFeeDueNoBulkRespondent() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        UUID uuid = createEntryNoRespondentWithOffsiteFeeForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // get the ids of the fee statuses
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();

        final EntryUpdateDto updateDto = createEntryUpdateDto(settings);
        // set the organisation and person applicant to null so we use the standard applicant
        updateDto.getApplicant().setOrganisation(null);
        updateDto.getApplicant().setPerson(null);
        updateDto.setFeeStatuses(null);
        updateDto.getRespondent().setOrganisation(null);
        updateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        updateDto.setNumberOfRespondents(0);

        // use the applicant standard applicant
        updateDto.setStandardApplicantCode("APP001");
        updateDto.setNumberOfRespondents(null);
        updateDto.setApplicationCode("MX99010");
        updateDto.setHasOffsiteFee(false);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Summarise offence title(s)");
        substitution.setValue("test wording");

        updateDto.setWordingFields(List.of(substitution));

        // execute the test
        PayloadForUpdateEntry payloadForCreate =
                new PayloadForUpdateEntry(
                        updateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());
        MatchResponse<EntryGetDetailDto> response =
                applicationEntryService.updateEntry(payloadForCreate);

        entityManager.clear();

        Assertions.assertEquals("MX99010", response.getPayload().getApplicationCode());
        Assertions.assertEquals("APP001", response.getPayload().getStandardApplicantCode());

        final List<AppListEntryFeeStatus> feeStatusesUpdated =
                appListEntryFeeStatusRepository.findByAppListEntryId(
                        applicationListEntry.get().getId());

        // make sure a no-fee update with null feeStatuses leaves fee-status history unchanged
        Assertions.assertEquals(feeStatusBeforeUpdate.size(), feeStatusesUpdated.size());

        for (Long id : feeStatusBeforeUpdate) {
            Assertions.assertTrue(
                    feeStatusesUpdated.stream().anyMatch(fs -> fs.getId().equals(id)),
                    "Expected fee status with id " + id + " to be preserved");
        }
    }

    @Test
    @Transactional
    void updateEntryWithCodeFeeReferencingOffsiteFeeExpectSingleFeeRecord() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // build the payload
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setApplicationCode("AD99002");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setWordingFields(null);
        entryUpdateDto.setHasOffsiteFee(true);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

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
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(Keyable::getId).toList();

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

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Request for copy documents on computer disc or in electronic form",
                "Request for copy documents on computer disc or in electronic form",
                List.of(),
                feeStatusBeforeUpdate,
                2);
    }

    @Test
    @Transactional
    void updateEntryWithCodeFeeNotReferencingOffsiteFeeButOffsiteFeeAttachedExpectTwoFeeRecords() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // build the payload
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);

        // no respondent for this code
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setApplicationCode("AD99002");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setHasOffsiteFee(true);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        entryUpdateDto.setWordingFields(null);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

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
        final List<Long> feeStatusBeforeUpdate = feeStatuses.stream().map(Keyable::getId).toList();
        final List<Long> feeOfficialBeforeUpdate =
                feeOfficial.stream().map(Keyable::getId).toList();

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

        applicationListEntry = applicationListEntryRepository.findByUuid(uuid);
        applicationListEntryAssertion.validateEntityAndResponseForEntryUpdate(
                new ApplicationListEntryWrapperDto(entryUpdateDto),
                applicationListEntry.get(),
                update.getPayload(),
                "Request for copy documents on computer disc or in electronic form",
                "Request for copy documents on computer disc or in electronic form",
                List.of(),
                feeStatusBeforeUpdate,
                2);
    }

    @Test
    @Transactional
    void updateEntryWithNullHasOffsiteFeeDoesNotThrow() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        // Create an entry that already exists
        UUID uuid = createEntryNoRespondentWithOffsiteFeeForTest();

        Optional<ApplicationListEntry> applicationListEntry =
                applicationListEntryRepository.findByUuid(uuid);

        Assertions.assertTrue(applicationListEntry.isPresent());

        // Build an update that goes through updateFees() and creates a new fee mapping
        EntryUpdateDto entryUpdateDto = createEntryUpdateDto(settings);

        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setApplicationCode("AD99002");
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setWordingFields(null);

        entryUpdateDto.setHasOffsiteFee(null);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryUpdateDto.getFeeStatuses());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(
                        entryUpdateDto,
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid());

        MatchResponse<EntryGetDetailDto> update =
                Assertions.assertDoesNotThrow(() -> applicationEntryService.updateEntry(payload));

        Assertions.assertNotNull(update.getEtag());

        List<Fee> fees =
                appListEntryFeeRepository.getFeeForEntryId(applicationListEntry.get().getId());

        // Should only have the main fee, and no offsite fee should be attached
        Assertions.assertEquals(1, fees.size());
        Assertions.assertFalse(fees.stream().anyMatch(Fee::isOffsite));
    }

    @Test
    void createEntryWithNullHasOffsiteFeeDoesNotThrow() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(null);

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(null);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        // run the test
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
                    return Assertions.assertDoesNotThrow(
                            () -> applicationEntryService.createEntry(payloadForCreate));
                });
    }

    // useful method to create an entry with respondent, bulk respondent and fee statuses for update
    // purposes

    /**
     * Creates an entry and returns the UUID.
     *
     * @return The UUID of the created entry
     */
    private UUID createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest() {
        return createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest(
                LocalDate.now(java.time.ZoneOffset.UTC));
    }

    private UUID createEntryWithBulkRespondentAndApplicantWithFeeStatusesForTest(
            LocalDate lodgementDate) {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.setOfficials(limitOfficials(entryCreateDto.getOfficials()));
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setLodgementDate(lodgementDate);

        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
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

        entryCreateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
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

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setHasOffsiteFee(true);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                                    + LocalDate.now(java.time.ZoneOffset.UTC)
                                    + "}",
                            "Application for a warrant to enter premises at "
                                    + "{{Premises Address}} for date {{Premises Date}}",
                            entryCreateDto.getWordingFields(),
                            1);
                });

        return response.getPayload().getId();
    }

    public UUID createEntryNoRespondentWithOffsiteFeeForTest() {
        // create the create entry payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        final EntryCreateDto entryCreateDto = createEntryCreateDto(settings);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getName().setMiddleName(JsonNullable.of(null));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC));

        // no respondent for this code
        entryCreateDto.setRespondent(null);
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.setWordingFields(null);
        entryCreateDto.setHasOffsiteFee(false);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

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
                    Assertions.assertTrue(
                            applicationListEntryRepository
                                    .findByUuid(response.getPayload().getId())
                                    .isPresent());
                });
        return response.getPayload().getId();
    }

    private EntryCreateDto createEntryCreateDto(Settings settings) {
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(limitOfficials(entryCreateDto.getOfficials()));
        return entryCreateDto;
    }

    private EntryUpdateDto createEntryUpdateDto(Settings settings) {
        EntryUpdateDto entryUpdateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();
        entryUpdateDto.setOfficials(limitOfficials(entryUpdateDto.getOfficials()));
        return entryUpdateDto;
    }

    private List<Official> limitOfficials(List<Official> officials) {
        if (officials == null || officials.isEmpty()) {
            return officials;
        }

        int magistrates = 0;
        int clerks = 0;
        List<Official> limitedOfficials = new java.util.ArrayList<>();

        for (Official official : officials) {
            if (official == null || official.getType() == null) {
                limitedOfficials.add(official);
                continue;
            }

            if (official.getType() == OfficialType.MAGISTRATE) {
                if (magistrates < 3) {
                    limitedOfficials.add(official);
                    magistrates++;
                }
                continue;
            }

            if (official.getType() == OfficialType.CLERK) {
                if (clerks < 1) {
                    limitedOfficials.add(official);
                    clerks++;
                }
                continue;
            }

            limitedOfficials.add(official);
        }

        return limitedOfficials;
    }
}
