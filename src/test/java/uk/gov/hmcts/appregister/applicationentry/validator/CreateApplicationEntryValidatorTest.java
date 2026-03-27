package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateApplicationEntryValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Mock private FeeRepository feeRepository;

    @Mock private Clock clock;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    @InjectMocks private CreateApplicationEntryValidator createApplicationEntryValidator;

    // data to be used in tests
    private EntryCreateDto entryCreateDto;
    private ApplicationCode applicationCode;
    private StandardApplicant standardApplicant;
    private Fee fee;
    private ApplicationList applicationList;
    private UUID appListUuid;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);

        AppListTestData appListTestData = new AppListTestData();
        applicationList = appListTestData.someComplete();
        applicationList.setDeleted(null);

        // make sure the application list is open
        applicationList.setStatus(Status.OPEN);

        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        applicationCode = applicationCodeTestData.someComplete();
        applicationCode.setFeeDue(YesOrNo.YES);
        applicationCode.setBulkRespondentAllowed(YesOrNo.YES);
        applicationCode.setRequiresRespondent(YesOrNo.YES);

        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();
        standardApplicant = standardApplicantTestData.someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        entryCreateDto = Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        appListUuid = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(appListUuid))
                .thenReturn(Optional.of(applicationList));
        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));

        FeeTestData feeTestData = new FeeTestData();
        fee = feeTestData.someComplete();
        fee.setId(1L);
        fee.setOffsite(true);

        when(feeRepository.findByReferenceBetweenDateWithOffsite(
                        eq(applicationCode.getFeeReference()),
                        notNull(),
                        eq(entryCreateDto.getHasOffsiteFee())))
                .thenReturn(List.of(fee));

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryCreateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of(standardApplicant));
    }

    @Test
    void testValidateSuccess() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);

        // set the respondent to null for the organisation so we use the person
        entryCreateDto.getRespondent().setOrganisation(null);

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        createApplicationEntryValidator.validate(payload);
    }

    @Test
    void testValidateSuccessForEnforcementCode() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);

        // set application code to match the application code in the repository
        entryCreateDto.setApplicationCode("EF12121");
        entryCreateDto.setAccountNumber("test");

        // set the respondent to null for the organisation so we use the person
        entryCreateDto.getRespondent().setOrganisation(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        createApplicationEntryValidator.validate(payload);
    }

    @Test
    void testRespondentMutualExclusiveFail() {
        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantMutualExclusiveFail() {
        entryCreateDto.getRespondent().setOrganisation(null);

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantFeeNotDueFail() {
        applicationCode.setFeeDue(YesOrNo.NO);

        FeeStatus feeStatus = new FeeStatus();
        entryCreateDto.setFeeStatuses(List.of(feeStatus));

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getApplicant().setOrganisation(null);

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.FEE_NOT_REQUIRED.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantFeeDueFail() {
        applicationCode.setFeeDue(YesOrNo.YES);

        entryCreateDto.setFeeStatuses(List.of());

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getApplicant().setOrganisation(null);

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.FEE_REQUIRED.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListNotExisting() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getApplicant().setOrganisation(null);

        when(applicationListRepository.findByUuidIncludingDelete(appListUuid))
                .thenReturn(Optional.empty());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListNotInCorrectStatus() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getApplicant().setOrganisation(null);

        applicationList.setStatus(Status.CLOSED);
        when(applicationListRepository.findByUuidIncludingDelete(appListUuid))
                .thenReturn(Optional.of(applicationList));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListDeleted() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getApplicant().setOrganisation(null);

        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.YES);

        when(applicationListRepository.findByUuid(appListUuid))
                .thenReturn(Optional.of(applicationList));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testStandardApplicantNotExist() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryCreateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testStandardApplicantMultiple() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryCreateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of(new StandardApplicant(), new StandardApplicant()));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.MULTIPLE_STANDARD_APPLICANT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantCodeNotExist() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantCodeMultiple() {
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().setPerson(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(new ApplicationCode(), new ApplicationCode()));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.MULTIPLE_APPLICATION_CODE_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testPaymentReferenceNotAllowedWhenPaymentDue() {
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);
        entryCreateDto.getRespondent().setOrganisation(null);

        // Ensure we have a fee status and set it to DUE with a payment reference (invalid)
        FeeStatus feeStatus = new FeeStatus();
        feeStatus.setPaymentStatus(DUE);
        feeStatus.setPaymentReference("PAYREF-123");
        feeStatus.setStatusDate(LocalDate.now(clock));

        entryCreateDto.setFeeStatuses(List.of(feeStatus));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // Act + Assert
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));

        Assertions.assertEquals(
                AppListEntryError.PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testValidateFailureForEnforcementApplicationCode() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);

        // set the EF application code so that we require the account number
        entryCreateDto.setApplicationCode("EF12121");
        entryCreateDto.setAccountNumber(null);

        // set the respondent to null for the organisation so we use the person
        entryCreateDto.getRespondent().setOrganisation(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> createApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_NUMBER_REQUIRED_FOR_APPLICATION_CODE,
                appRegistryException.getCode());
    }

    @Test
    void testRespondentAllowedWhenNotRequired_Success() {
        // Arrange: application code says respondent is NOT required
        applicationCode.setRequiresRespondent(YesOrNo.NO);

        // Make the DTO valid in other respects so we only test the respondent rule
        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.setStandardApplicantCode(null);

        // Ensure respondent exists (payload includes respondent)
        Assertions.assertNotNull(
                entryCreateDto.getRespondent(), "Test requires respondent to be present");
        entryCreateDto
                .getRespondent()
                .setOrganisation(null); // use person respondent to avoid mutual-exclusive failure

        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(entryCreateDto.getFeeStatuses());

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(appListUuid)
                        .data(entryCreateDto)
                        .build();

        Assertions.assertDoesNotThrow(() -> createApplicationEntryValidator.validate(payload));
    }
}
