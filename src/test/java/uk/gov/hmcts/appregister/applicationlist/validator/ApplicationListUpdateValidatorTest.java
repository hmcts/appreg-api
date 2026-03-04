package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

@ExtendWith(MockitoExtension.class)
public class ApplicationListUpdateValidatorTest {

    @Mock private ApplicationListRepository repository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResolutionRepository;
    @Mock private AppListEntryOfficialRepository appListEntryOfficialRepository;
    @Mock private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;
    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @InjectMocks private ApplicationUpdateListLocationValidator validator;

    private enum Field {
        COURT,
        CJA,
        OTHER
    }

    // ---- HELPERS ----
    private ApplicationListUpdateDto buildDto(Field... fields) {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();

        for (Field f : fields) {
            switch (f) {
                case COURT -> {
                    dto.setCourtLocationCode("COURT-123");
                }
                case CJA -> {
                    dto.setCjaCode("CJA-123");
                }
                case OTHER -> dto.setOtherLocationDescription("Some other location");
            }
        }
        return dto;
    }

    @Test
    void update_noCourtReturnedFromRepository_throwsAppRegException() {
        // given
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("CODE1");
        when(courtHouseRepository.findActiveCourts("CODE1")).thenReturn(List.of());

        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        // expect
        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No court found");
    }

    @Test
    void update_multipleCourtsReturnedFromRepository_throwsAppRegException() {
        // given
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("DUPE");

        NationalCourtHouse c1 = new NationalCourtHouse();
        NationalCourtHouse c2 = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("DUPE")).thenReturn(List.of(c1, c2));

        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        // expect
        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Multiple courts found");
    }

    @Test
    void update_noCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);

        when(dto.getCourtLocationCode()).thenReturn(null);
        when(dto.getCjaCode()).thenReturn("X1");
        when(dto.getOtherLocationDescription()).thenReturn("Y2");

        when(cjaRepository.findByCode("X1")).thenReturn(List.of());
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(ApplicationListError.CJA_NOT_FOUND, exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void update_multipleCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        when(dto.getCourtLocationCode()).thenReturn(null);
        when(dto.getCjaCode()).thenReturn("X1");
        when(dto.getOtherLocationDescription()).thenReturn("Y2");

        CriminalJusticeArea a = new CriminalJusticeArea();
        CriminalJusticeArea b = new CriminalJusticeArea();
        when(cjaRepository.findByCode("X1")).thenReturn(List.of(a, b));

        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(ApplicationListError.DUPLICATE_CJA_FOUND, exception.getCode());

        verify(repository, never()).save(any());
    }

    @Test
    void update_noAppList_throwsAppRegException() {
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);

        UUID uuid = UUID.randomUUID();
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListError.APPLICATION_LIST_NOT_FOUND, exception.getCode());
    }

    @Test
    void updateClosedListThrowsException() {
        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.CLOSED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(ApplicationListError.INVALID_LIST_STATUS, exception.getCode());
    }

    @Test
    void successCloseUpdate() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(21);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));

        // make sure that the national court resolves
        NationalCourtHouse court = mock(NationalCourtHouse.class);
        when(courtHouseRepository.findActiveCourts(courtCode)).thenReturn(List.of(court));

        ApplicationListEntry listEntry = new ApplicationListEntry();

        // The entry has a fee due, which should trigger the not paid validation failure
        ApplicationCode applicationCode = new ApplicationCode();
        applicationCode.setFeeDue(YesOrNo.YES);

        listEntry.setApplicationCode(applicationCode);
        UUID entryUuid = UUID.randomUUID();
        listEntry.setUuid(entryUuid);

        when(applicationListEntryRepository.findByApplicationListId(appList.getId()))
                .thenReturn(List.of(listEntry));

        when(appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryResolution()));

        when(appListEntryOfficialRepository.getOfficialByEntryUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryOfficial()));

        AppListEntryFeeStatus appListEntryFeeStatus = new AppListEntryFeeStatus();

        // The fee is not paid so should fail.
        appListEntryFeeStatus.setAlefsFeeStatus(FeeStatusType.PAID);

        when(appListEntryFeeStatusRepository.findByAppListEntryId(listEntry.getId()))
                .thenReturn(List.of(appListEntryFeeStatus));

        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        // no exception should be thrown as all validations are met for closing the list
        validator.validate(payload);
    }

    @Test
    void updateClosedListThrowsDurationException() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(0);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));
        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_DURATION, exception.getCode());
    }

    @Test
    void updateClosedListThrowsNotResulted() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(21);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));

        ApplicationListEntry listEntry = new ApplicationListEntry();
        UUID entryUuid = UUID.randomUUID();
        listEntry.setUuid(entryUuid);

        when(applicationListEntryRepository.findByApplicationListId(appList.getId()))
                .thenReturn(List.of(listEntry));

        // no resolution or official records for the entry, which should trigger the duration
        // validation failure
        when(appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid()))
                .thenReturn(List.of());

        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NOT_RESULTED, exception.getCode());
    }

    @Test
    void updateClosedListThrowsNoOfficials() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(21);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));

        ApplicationListEntry listEntry = new ApplicationListEntry();
        UUID entryUuid = UUID.randomUUID();
        listEntry.setUuid(entryUuid);

        when(applicationListEntryRepository.findByApplicationListId(appList.getId()))
                .thenReturn(List.of(listEntry));

        when(appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryResolution()));

        when(appListEntryOfficialRepository.getOfficialByEntryUuid(listEntry.getUuid()))
                .thenReturn(List.of());

        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NO_OFFICIAL, exception.getCode());
    }

    @Test
    void updateClosedListThrowsNotSettled() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(21);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));

        ApplicationListEntry listEntry = new ApplicationListEntry();

        // The entry has a fee due, which should trigger the not paid validation failure
        ApplicationCode applicationCode = new ApplicationCode();
        applicationCode.setFeeDue(YesOrNo.YES);

        listEntry.setApplicationCode(applicationCode);
        UUID entryUuid = UUID.randomUUID();
        listEntry.setUuid(entryUuid);

        when(applicationListEntryRepository.findByApplicationListId(appList.getId()))
                .thenReturn(List.of(listEntry));

        when(appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryResolution()));

        when(appListEntryOfficialRepository.getOfficialByEntryUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryOfficial()));

        AppListEntryFeeStatus appListEntryFeeStatus = new AppListEntryFeeStatus();

        // The fee is not paid so should fail.
        appListEntryFeeStatus.setAlefsFeeStatus(FeeStatusType.DUE);

        when(appListEntryFeeStatusRepository.findByAppListEntryId(listEntry.getId()))
                .thenReturn(List.of(appListEntryFeeStatus));

        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NOT_SETTLED, exception.getCode());
    }

    @Test
    void updateClosedListWithRemittedFee_succeeds() {
        ApplicationListUpdateDto dto = new ApplicationListUpdateDto();
        dto.setDurationHours(0);
        dto.setDurationMinutes(21);
        dto.setStatus(ApplicationListStatus.CLOSED);

        // set the court code
        String courtCode = "court-code";
        dto.setCourtLocationCode(courtCode);

        UUID uuid = UUID.randomUUID();
        ApplicationList appList = new ApplicationList();
        appList.setStatus(Status.OPEN);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(appList));

        NationalCourtHouse court = mock(NationalCourtHouse.class);
        when(courtHouseRepository.findActiveCourts(courtCode)).thenReturn(List.of(court));

        ApplicationListEntry listEntry = new ApplicationListEntry();

        // The entry has a fee due, which should trigger the settled validation branch
        ApplicationCode applicationCode = new ApplicationCode();
        applicationCode.setFeeDue(YesOrNo.YES);

        listEntry.setApplicationCode(applicationCode);
        UUID entryUuid = UUID.randomUUID();
        listEntry.setUuid(entryUuid);

        when(applicationListEntryRepository.findByApplicationListId(appList.getId()))
                .thenReturn(List.of(listEntry));

        // resolution and official exist for the entry
        when(appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryResolution()));
        when(appListEntryOfficialRepository.getOfficialByEntryUuid(listEntry.getUuid()))
                .thenReturn(List.of(new AppListEntryOfficial()));

        AppListEntryFeeStatus appListEntryFeeStatus = new AppListEntryFeeStatus();

        // Set the fee status to REMITTED which should be accepted for closing
        appListEntryFeeStatus.setAlefsFeeStatus(FeeStatusType.REMITTED);

        when(appListEntryFeeStatusRepository.findByAppListEntryId(listEntry.getId()))
                .thenReturn(List.of(appListEntryFeeStatus));

        PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

        assertDoesNotThrow(() -> validator.validate(payload));
    }

    // ---- TESTS ----
    @Nested
    class ValidCombinations {

        @Test
        void valid_whenCourtLocationPresent_only() {
            var appList = buildDto(Field.COURT);
            when(courtHouseRepository.findActiveCourts(appList.getCourtLocationCode()))
                    .thenReturn(List.of(new NationalCourtHouse()));

            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            assertDoesNotThrow(() -> validator.validate(payload));
        }

        @Test
        void valid_whenCourtLocationPresentWithCallback_only() {
            var appList = buildDto(Field.COURT);
            when(courtHouseRepository.findActiveCourts(appList.getCourtLocationCode()))
                    .thenReturn(List.of(new NationalCourtHouse()));

            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            BiFunction<PayloadForUpdate<ApplicationListUpdateDto>, ListUpdateValidationSuccess, ?>
                    callback = (dto, success) -> "result";
            assertEquals("result", validator.validate(payload, callback));
        }

        @Test
        void valid_whenCjaAndNonBlankOtherLocation_andNoCourtLocation() {
            var appList = buildDto(Field.CJA, Field.OTHER);
            when(cjaRepository.findByCode(appList.getCjaCode()))
                    .thenReturn(List.of(new CriminalJusticeArea()));

            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            assertDoesNotThrow(() -> validator.validate(payload));
        }

        @Test
        void valid_whenCjaAndNonBlankOtherLocationWithCallback_andNoCourtLocation() {
            var appList = buildDto(Field.CJA, Field.OTHER);
            when(cjaRepository.findByCode(appList.getCjaCode()))
                    .thenReturn(List.of(new CriminalJusticeArea()));

            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            BiFunction<PayloadForUpdate<ApplicationListUpdateDto>, ListUpdateValidationSuccess, ?>
                    callback = (dto, success) -> "result";
            assertEquals("result", validator.validate(payload, callback));
        }
    }

    @Nested
    class InvalidCombinations {

        @Test
        void invalid_whenNothingProvided() {
            var appList = buildDto();
            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            AppRegistryException ex =
                    assertThrows(AppRegistryException.class, () -> validator.validate(payload));
            assertEquals(ApplicationListError.INVALID_LOCATION_COMBINATION, ex.getCode());
        }

        @Test
        void invalid_whenOnlyCjaProvided() {
            var appList = buildDto(Field.CJA);
            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        }

        @Test
        void invalid_whenOnlyOtherLocationProvided() {
            var appList = buildDto(Field.OTHER);
            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload =
                    new PayloadForUpdate<>(appList, uuid);

            assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        }

        @Test
        void invalid_whenAllFieldsProvided() {
            var dto = buildDto(Field.COURT, Field.CJA, Field.OTHER);

            UUID uuid = UUID.randomUUID();
            when(repository.findByUuid(uuid)).thenReturn(Optional.of(new ApplicationList()));
            PayloadForUpdate<ApplicationListUpdateDto> payload = new PayloadForUpdate<>(dto, uuid);

            assertThrows(AppRegistryException.class, () -> validator.validate(payload));
        }
    }
}
