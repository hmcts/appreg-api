package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationEntryResultUpdateValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Mock private ResolutionCodeRepository resolutionCodeRepository;

    @Mock private AppListEntryResolutionRepository appListEntryResolutionRepository;

    @InjectMocks private ApplicationEntryResultUpdateValidator validator;

    private UUID applicationListUuid;
    private UUID applicationListEntryUuid;
    private UUID resultUuid;

    private ResultUpdateDto dto;
    private PayloadForUpdateEntryResult payload;

    @BeforeEach
    void setUp() {
        applicationListUuid = UUID.randomUUID();
        applicationListEntryUuid = UUID.randomUUID();
        resultUuid = UUID.randomUUID();

        dto = new ResultUpdateDto();
        dto.setResultCode("RES_CODE");

        payload =
                new PayloadForUpdateEntryResult(
                        dto, applicationListUuid, applicationListEntryUuid, resultUuid);

        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(OPEN);

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        ResolutionCode resolutionCode = new ResolutionCode();

        // Make the ResolutionCode wording parseable (avoid template parsing errors)
        resolutionCode.setWording("Some wording");

        // ---- base validations (AbstractApplicationEntryResultValidator) ----
        when(applicationListRepository.findByUuidIncludingDelete(eq(applicationListUuid)))
                .thenReturn(Optional.of(applicationList));

        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(applicationListEntryUuid), eq(applicationListUuid)))
                .thenReturn(Optional.of(applicationListEntry));

        when(resolutionCodeRepository.findPrioritisingNullEndDate(
                        eq(dto.getResultCode()), any(Pageable.class)))
                .thenReturn(List.of(resolutionCode));

        // ---- additional validation in ApplicationEntryResultUpdateValidator ----
        AppListEntryResolution entryResolution = new AppListEntryResolution();
        when(appListEntryResolutionRepository.findByUuidAndApplicationList_Uuid(
                        eq(resultUuid), eq(applicationListEntryUuid)))
                .thenReturn(Optional.of(entryResolution));
    }

    @Test
    void validateSuccess() {
        validator.validate(payload);
    }

    @Test
    void validateEntryResultDoesNotExist() {
        when(appListEntryResolutionRepository.findByUuidAndApplicationList_Uuid(
                        eq(resultUuid), eq(applicationListEntryUuid)))
                .thenReturn(Optional.empty());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }

    @Test
    void validateApplicationListDoesNotExist() {
        when(applicationListRepository.findByUuidIncludingDelete(eq(applicationListUuid)))
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
    void validateApplicationListEntryDoesNotExist() {
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(applicationListEntryUuid), eq(applicationListUuid)))
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
    void validateResolutionCodeDoesNotExist() {
        when(resolutionCodeRepository.findPrioritisingNullEndDate(
                        eq(dto.getResultCode()), any(Pageable.class)))
                .thenReturn(List.of());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));

        Assertions.assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST
                        .getCode()
                        .getAppCode(),
                ex.getCode().getCode().getAppCode());
    }
}
