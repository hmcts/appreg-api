package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

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
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateApplicationEntryValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Mock private FeeRepository feeRepository;

    @Mock private Clock clock;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private UpdateApplicationEntryValidator updateApplicationEntryValidator;

    // data to be used in tests
    private EntryUpdateDto entryUpdateDto;
    private ApplicationCode applicationCode;
    private StandardApplicant standardApplicant;
    private Fee fee;
    private ApplicationList applicationList;
    private UUID appListUuid;
    private UUID appListEntryUuid;

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

        FeeTestData feeTestData = new FeeTestData();
        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();

        fee = feeTestData.someComplete();
        standardApplicant = standardApplicantTestData.someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        entryUpdateDto = Instancio.of(EntryUpdateDto.class).withSettings(settings).create();

        appListUuid = UUID.randomUUID();
        appListEntryUuid = UUID.randomUUID();

        when(applicationListRepository.findByUuid(appListUuid))
                .thenReturn(Optional.of(applicationList));
        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryUpdateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));
        when(feeRepository.findByReferenceBetweenDateWithOffsite(
                        eq(applicationCode.getFeeReference()),
                        notNull(),
                        eq(entryUpdateDto.getHasOffsiteFee())))
                .thenReturn(List.of(fee));

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryUpdateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of(standardApplicant));

        when(applicationListEntryRepository.findByUuid(eq(appListEntryUuid)))
                .thenReturn(Optional.of(new ApplicationListEntry()));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        eq(appListUuid), eq(appListEntryUuid)))
                .thenReturn(Optional.of(new ApplicationListEntry()));
    }

    @Test
    void testValidateSuccess() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);

        // set the respondent to null for the organisation so we use the person
        entryUpdateDto.getRespondent().setOrganisation(null);

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        updateApplicationEntryValidator.validate(payload);
    }

    @Test
    void testValidateEntryDoesNotExist() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);

        // set the respondent to null for the organisation so we use the person
        entryUpdateDto.getRespondent().setOrganisation(null);

        when(applicationListEntryRepository.findByUuid(eq(appListEntryUuid)))
                .thenReturn(Optional.empty());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType(),
                appRegistryException.getCode().getCode().getType());
    }

    @Test
    void testValidateEntryNotInList() {
        // set the applicant to null for the organisation and standard applicant so we use the
        // person
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);

        // set the respondent to null for the organisation so we use the person
        entryUpdateDto.getRespondent().setOrganisation(null);

        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        eq(appListUuid), eq(appListEntryUuid)))
                .thenReturn(Optional.empty());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType(),
                appRegistryException.getCode().getCode().getType());
    }

    @Test
    void testRespondentMutualExclusiveFail() {
        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantMutualExclusiveFail() {
        entryUpdateDto.getRespondent().setOrganisation(null);

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
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
        entryUpdateDto.setFeeStatuses(List.of(feeStatus));

        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.FEE_NOT_REQUIRED.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantFeeDueFail() {
        applicationCode.setFeeDue(YesOrNo.YES);

        entryUpdateDto.setFeeStatuses(List.of());

        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.FEE_REQUIRED.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListNotExisting() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        when(applicationListRepository.findByUuid(appListUuid)).thenReturn(Optional.empty());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListNotInCorrectStatus() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        applicationList.setStatus(Status.CLOSED);
        when(applicationListRepository.findByUuid(appListUuid))
                .thenReturn(Optional.of(applicationList));

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantListDeleted() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.YES);

        when(applicationListRepository.findByUuid(appListUuid))
                .thenReturn(Optional.of(applicationList));

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testStandardApplicantNotExist() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().setPerson(null);

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryUpdateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testStandardApplicantMultiple() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().setPerson(null);

        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(
                        entryUpdateDto.getStandardApplicantCode(), LocalDate.now(clock)))
                .thenReturn(List.of(new StandardApplicant(), new StandardApplicant()));

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.MULTIPLE_STANDARD_APPLICANT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantCodeNotExist() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().setPerson(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryUpdateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of());

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }

    @Test
    void testApplicantCodeMultiple() {
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getApplicant().setOrganisation(null);
        entryUpdateDto.getApplicant().setPerson(null);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryUpdateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(new ApplicationCode(), new ApplicationCode()));

        PayloadForUpdateEntry payload =
                new PayloadForUpdateEntry(entryUpdateDto, appListUuid, appListEntryUuid);

        // validate the payload
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> updateApplicationEntryValidator.validate(payload));
        Assertions.assertEquals(
                AppListEntryError.MULTIPLE_APPLICATION_CODE_EXIST.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
    }
}
