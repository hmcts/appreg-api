package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationEntryResultCreationValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private ResolutionCodeRepository resolutionCodeRepository;

    @InjectMocks private ApplicationEntryResultCreationValidator validator;

    private PayloadForCreateEntryResult<ResultCreateDto> payload;
    private ResultCreateDto dto;

    private UUID listId;
    private UUID entryId;

    private ApplicationList list;
    private ResolutionCode resolutionCode;

    @BeforeEach
    void setUp() {
        // Dto
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        dto = Instancio.of(ResultCreateDto.class).withSettings(settings).create();

        listId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        // Entities
        list = Instancio.create(ApplicationList.class);
        list.setUuid(listId);
        list.setStatus(Status.OPEN);
        list.setDeleted(null);

        ApplicationListEntry entry = Instancio.create(ApplicationListEntry.class);
        entry.setUuid(entryId);

        resolutionCode = Instancio.create(ResolutionCode.class);
        resolutionCode.setResultCode(dto.getResultCode());
        resolutionCode.setWording("Outcome: {{resultCode}}");

        payload =
                PayloadForCreateEntryResult.<ResultCreateDto>builder()
                        .listId(listId)
                        .entryId(entryId)
                        .data(dto)
                        .build();

        // Happy-path stubs
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(list));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(entryId, listId))
                .thenReturn(Optional.of(entry));
        when(resolutionCodeRepository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                        dto.getResultCode(), PageRequest.of(0, 1)))
                .thenReturn(List.of(resolutionCode));
    }

    @Test
    void validate_success() {
        validator.validate(payload);
    }

    @Test
    void validate_applicationListDoesNotExist() {
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validate_applicationListNotOpen() {
        list.setStatus(Status.CLOSED);
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(list));

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validate_applicationListDeleted() {
        list.setStatus(Status.OPEN);
        list.setDeleted(YesOrNo.YES);
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(list));

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validate_entryDoesNotExistOrNotBelongToList() {
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(entryId), eq(listId)))
                .thenReturn(Optional.empty());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validate_resolutionCodeDoesNotExist() {
        when(resolutionCodeRepository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                        dto.getResultCode(), PageRequest.of(0, 1)))
                .thenReturn(Collections.emptyList());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validate_wordingTemplateMalformed_shouldRecordErroneousTemplates_andNotThrow() {
        // This will match the { ... } regex, but fail WordingTemplate.WordingTemplate(PATTERN)
        // because it's not "TYPE|REFERENCE|LENGTH" with valid LENGTH etc.
        resolutionCode.setWording("Some text {NOT_A_VALID_TEMPLATE} end.");

        when(resolutionCodeRepository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                        dto.getResultCode(), PageRequest.of(0, 1)))
                .thenReturn(List.of(resolutionCode));

        ListEntryResultCreateValidationSuccess success = validator.validate(payload, (v, s) -> s);

        // And it should have recorded the bad template content (group(1) inside the braces)
        Assertions.assertFalse(
                success.getWordingSentence().getErroneousTemplates().isEmpty(),
                "Expected erroneous templates to be recorded");

        Assertions.assertEquals(
                "NOT_A_VALID_TEMPLATE",
                success.getWordingSentence().getErroneousTemplates().getFirst());
    }
}
