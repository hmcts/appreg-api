package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@ExtendWith(MockitoExtension.class)
public class ActionServiceImplTest {

    @Mock private ApplicationListRepository alRepository;
    @Mock private ApplicationListEntryRepository aleRepository;

    @Spy
    private DummyMoveEntriesValidator moveEntriesValidator =
        new DummyMoveEntriesValidator(alRepository, aleRepository);

    private ActionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActionServiceImpl(aleRepository, moveEntriesValidator);
    }

    @Captor private ArgumentCaptor<List<ApplicationListEntry>> listCaptor;

    @Test
    void move_movesEntriesSuccessfully_whenValidRequest() {
        // Arrange: build DTO and entries
        UUID sourceListId = UUID.randomUUID();

        ApplicationList sourceList = new ApplicationList();
        sourceList.setUuid(sourceListId);

        ApplicationList targetList = new ApplicationList();
        targetList.setUuid(UUID.randomUUID());

        ApplicationListEntry entry1 = new ApplicationListEntry();
        entry1.setUuid(UUID.randomUUID());
        entry1.setApplicationList(targetList); // what will be persisted

        ApplicationListEntry entry2 = new ApplicationListEntry();
        entry2.setUuid(UUID.randomUUID());
        entry2.setApplicationList(targetList); // what will be persisted

        // DTO
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(entry1.getUuid(), entry2.getUuid()));

        // Short-circuit the validator with a pre-built success
        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setSourceList(sourceList);
        success.setTargetList(targetList);
        success.setEntriesToSave(List.of(entry1, entry2));

        moveEntriesValidator.setSuccess(success);

        // Act
        service.move(sourceListId, dto);

        // Assert: repository saveAll called with the expected entries
        verify(aleRepository).saveAll(listCaptor.capture());
        List<ApplicationListEntry> capturedList = listCaptor.getValue();

        Assertions.assertEquals(2, capturedList.size());
        Assertions.assertTrue(capturedList.contains(entry1));
        Assertions.assertTrue(capturedList.contains(entry2));
    }

    @Test
    void move_returns404_whenSourceListDoesNotExist() {
        // Validator will throw the expected AppRegistryException
        doThrow(
            new AppRegistryException(
                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                "No source application list found for UUID"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns404_whenTargetListDoesNotExist() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.TARGET_LIST_NOT_FOUND,
                "No target application list found for UUID"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenSourceListNotOpen() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.INVALID_LIST_STATUS,
                "Source list not open"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenTargetListNotOpen() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.INVALID_LIST_STATUS,
                "Target list not open"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsNull() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.ENTRY_NOT_PROVIDED,
                "No entry IDs provided"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsEmpty() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.ENTRY_NOT_PROVIDED,
                "No entry IDs provided"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of());

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns404_whenEntryDoesNotExist() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.ENTRY_NOT_FOUND,
                "No application list entry found"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenEntryNotInSourceList() {
        doThrow(
            new AppRegistryException(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                "Application list entry does not belong to source list"))
            .when(moveEntriesValidator)
            .validate(any(MoveEntriesDto.class), any());

        UUID sourceListId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(sourceListId, dto))
            .isInstanceOf(AppRegistryException.class)
            .extracting(e -> ((AppRegistryException)e).getCode().getCode().getHttpCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Setter
    static class DummyMoveEntriesValidator extends MoveEntriesValidator {

        private MoveEntriesValidationSuccess success;

        public DummyMoveEntriesValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository) {
            super(applicationListRepository, applicationListEntryRepository);
        }

        @Override
        public <R> R validate(
            MoveEntriesDto dto,
            java.util.function.BiFunction<MoveEntriesDto, MoveEntriesValidationSuccess, R> createSupplier) {

            return createSupplier.apply(dto, success);
        }

        public DummyMoveEntriesValidator withSourceList(UUID id) {
            return this;
        }
    }
}
