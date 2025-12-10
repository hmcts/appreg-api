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

        FeeTestData feeTestData = new FeeTestData();
        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();

        fee = feeTestData.someComplete();
        standardApplicant = standardApplicantTestData.someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        entryCreateDto = Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        appListUuid = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(appListUuid))
                .thenReturn(Optional.of(applicationList));
        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));
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
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
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
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
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
}
