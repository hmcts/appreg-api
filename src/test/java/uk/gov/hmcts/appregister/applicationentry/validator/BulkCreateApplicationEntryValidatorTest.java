package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkCreateApplicationEntryValidatorTest {
    private static final LocalDate TODAY_UK = LocalDate.of(2025, Month.OCTOBER, 7);

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Mock private ApplicationFeeService feeService;

    @Mock private BusinessDateProvider businessDateProvider;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    private BulkCreateApplicationEntryValidator validator;

    private EntryCreateDto entryCreateDto;
    private ApplicationCode applicationCode;
    private StandardApplicant standardApplicant;
    private Fee fee;
    private ApplicationList applicationList;
    private UUID appListUuid;

    @BeforeEach
    void setUp() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        applicationList = new AppListTestData().someComplete();
        applicationList.setDeleted(null);
        applicationList.setStatus(Status.OPEN);

        applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setFeeDue(YesOrNo.YES);
        applicationCode.setBulkRespondentAllowed(YesOrNo.YES);
        applicationCode.setRequiresRespondent(YesOrNo.YES);
        applicationCode.setFeeReference("TEST-FEE");
        applicationCode.setWording("Test template");

        standardApplicant = new StandardApplicantTestData().someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        entryCreateDto = Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(CreateEntryDtoUtil.validOfficials());

        appListUuid = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(appListUuid))
                .thenReturn(Optional.of(applicationList));
        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(applicationCode));
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate("APP001", TODAY_UK))
                .thenReturn(List.of(standardApplicant));

        fee = new FeeTestData().someComplete();
        fee.setId(1L);
        fee.setOffsite(true);

        when(feeService.resolveFeePair(notNull())).thenReturn(new FeePair(null, fee));
        when(feeService.resolveFeePair(notNull(), notNull())).thenReturn(new FeePair(null, fee));

        validator =
                new BulkCreateApplicationEntryValidator(
                        applicationListRepository,
                        applicationCodeRepository,
                        feeService,
                        businessDateProvider,
                        standardApplicantRepository);
    }

    @Test
    void testDoesNotRequireFeeStatusForFeeDueCode() {
        applicationCode.setFeeDue(YesOrNo.YES);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);

        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setHasOffsiteFee(true);

        CreateApplicationEntryValidationSuccess success =
                validator.validate(payload(), (validatable, result) -> result);

        Assertions.assertSame(applicationCode, success.getApplicationCode());
        Assertions.assertSame(applicationList, success.getApplicationList());
        Assertions.assertSame(standardApplicant, success.getSa());
        Assertions.assertSame(fee, success.getFee().offsiteFee());
    }

    @Test
    void givenJobSession_whenValidatingRepeatedReferences_thenLoadsAndResolvesOnce() {
        applicationCode.setCode(entryCreateDto.getApplicationCode());
        standardApplicant.setApplicantCode("APP001");
        applicationCode.setFeeDue(YesOrNo.YES);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setHasOffsiteFee(true);
        when(applicationCodeRepository.findAllByDate(TODAY_UK))
                .thenReturn(List.of(applicationCode));
        when(standardApplicantRepository.findAllByDate(TODAY_UK))
                .thenReturn(List.of(standardApplicant));

        var session = validator.createSession(applicationList);
        var first = session.validate(payload(), (validatable, result) -> result);
        var second = session.validate(payload(), (validatable, result) -> result);

        assertThat(first.getApplicationCode()).isSameAs(applicationCode);
        assertThat(second.getSa()).isSameAs(standardApplicant);
        verify(applicationCodeRepository).findAllByDate(TODAY_UK);
        verify(standardApplicantRepository).findAllByDate(TODAY_UK);
        verify(feeService, times(1))
                .resolveFeePair(applicationCode.getFeeReference(), TODAY_UK.minusDays(1));
        verify(applicationListRepository, never()).findByUuidIncludingDelete(appListUuid);
        verify(applicationCodeRepository, never()).findByCodeAndDate(notNull(), notNull());
        verify(standardApplicantRepository, never())
                .findStandardApplicantByCodeAndDate(notNull(), notNull());
    }

    @Test
    void testValidateApplicationListThrowsWhenApplicationListDoesNotExist() {
        UUID missingListUuid = UUID.randomUUID();
        when(applicationListRepository.findByUuidIncludingDelete(missingListUuid))
                .thenReturn(Optional.empty());

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validateApplicationList(missingListUuid));

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST, appRegistryException.getCode());
        assertThat(appRegistryException.getMessage())
                .isEqualTo("The application list does not exist %s".formatted(missingListUuid));
    }

    @Test
    void testTrimsApplicationTextToWordingTemplatePlaceholders() {
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
        applicationCode.setWording(
                "Test template {TEXT|Applicant officer|10} and second template "
                        + "{TEXT|Applicant solicitor|10}");

        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution(null, "one"),
                        new TemplateSubstitution(null, "two"),
                        new TemplateSubstitution(null, "three")));

        CreateApplicationEntryValidationSuccess success =
                validator.validate(payload(), (validatable, result) -> result);

        Assertions.assertEquals(2, entryCreateDto.getWordingFields().size());
        Assertions.assertEquals(
                "Applicant officer", entryCreateDto.getWordingFields().get(0).getKey());
        Assertions.assertEquals("one", entryCreateDto.getWordingFields().get(0).getValue());
        Assertions.assertEquals(
                "Applicant solicitor", entryCreateDto.getWordingFields().get(1).getKey());
        Assertions.assertEquals("two", entryCreateDto.getWordingFields().get(1).getValue());
        Assertions.assertEquals(
                "Test template {one} and second template {two}",
                success.getWordingSentence()
                        .substitute(entryCreateDto.getWordingFields())
                        .getSubstitutedString());
    }

    @Test
    void testSkipsApplicationTextForWordingTemplateWithoutPlaceholders() {
        applicationCode.setCode("AD99001");
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
        applicationCode.setWording("Request to copy documents");

        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setWordingFields(
                List.of(new TemplateSubstitution(null, ""), new TemplateSubstitution(null, "")));

        when(applicationCodeRepository.findByCodeAndDate(eq("AD99001"), notNull()))
                .thenReturn(List.of(applicationCode));

        CreateApplicationEntryValidationSuccess success =
                validator.validate(payload(), (validatable, result) -> result);

        assertThat(entryCreateDto.getWordingFields()).isEmpty();
        Assertions.assertEquals(
                "Request to copy documents",
                success.getWordingSentence()
                        .substitute(entryCreateDto.getWordingFields())
                        .getSubstitutedString());
    }

    @Test
    void testRequiresEnoughApplicationTextForWordingTemplatePlaceholders() {
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
        applicationCode.setWording(
                "Test template {TEXT|Applicant officer|10} and second template "
                        + "{TEXT|Applicant solicitor|10}");

        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setWordingFields(List.of(new TemplateSubstitution(null, "one")));
        PayloadForCreate<EntryCreateDto> payload = payload();

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload, (validatable, result) -> result));

        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    void testRequiresNonBlankApplicationTextForWordingTemplatePlaceholders() {
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
        applicationCode.setWording("Test template {TEXT|Applicant officer|10}");

        entryCreateDto.setApplicant(null);
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setLodgementDate(TODAY_UK.minusDays(1));
        entryCreateDto.setWordingFields(List.of(new TemplateSubstitution(null, "")));
        PayloadForCreate<EntryCreateDto> payload = payload();

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload, (validatable, result) -> result));

        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    private PayloadForCreate<EntryCreateDto> payload() {
        return PayloadForCreate.<EntryCreateDto>builder()
                .id(appListUuid)
                .data(entryCreateDto)
                .build();
    }
}
