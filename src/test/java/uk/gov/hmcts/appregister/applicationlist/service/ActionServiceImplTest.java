package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@ExtendWith(MockitoExtension.class)
public class ActionServiceImplTest {

    @Mock private ApplicationListRepository alRepository;
    @Mock private ApplicationListEntryRepository aleRepository;

    @Spy
    private DummyMoveEntriesValidator moveEntriesValidator =
            new DummyMoveEntriesValidator(alRepository);

    private ActionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActionServiceImpl(aleRepository, moveEntriesValidator);
    }

    @Test
    void move_performsBulkUpdate_whenValidRequest() {
        ApplicationList targetList = new ApplicationList();
        targetList.setUuid(UUID.randomUUID());

        // Two entry UUIDs requested
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(id1, id2));

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);
        moveEntriesValidator.setSuccess(success);

        // Mock repository to return rowsUpdated == requested size (2)
        UUID sourceListId = UUID.randomUUID();
        when(aleRepository.bulkMoveByUuidAndSourceList(anySet(), eq(targetList), eq(sourceListId)))
                .thenReturn(2);

        // Act - should not throw
        service.move(sourceListId, dto);

        // Verify the bulk update call was invoked once with the same source and target that the
        // service was called with
        verify(aleRepository, times(1))
                .bulkMoveByUuidAndSourceList(anySet(), eq(targetList), eq(sourceListId));
    }

    @Test
    void move_throws_whenBulkUpdateAffectsFewerRowsThanRequested() {
        ApplicationList targetList = new ApplicationList();
        targetList.setUuid(UUID.randomUUID());

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(id1, id2));

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);
        moveEntriesValidator.setSuccess(success);

        // Simulate DB updated only 1 row even though 2 were requested
        UUID sourceListId = UUID.randomUUID();
        when(aleRepository.bulkMoveByUuidAndSourceList(anySet(), eq(targetList), eq(sourceListId)))
                .thenReturn(1);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void move_returns404_whenSourceListDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                "No source application list found for UUID"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns404_whenTargetListDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.TARGET_LIST_NOT_FOUND,
                                "No target application list found for UUID"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenSourceListNotOpen() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.INVALID_LIST_STATUS, "Source list not open"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenTargetListNotOpen() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.INVALID_LIST_STATUS, "Target list not open"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsNull() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsEmpty() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                "No application list entry found"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryNotInSourceList() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                "Application list entry does not belong to source list"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Setter
    static class DummyMoveEntriesValidator extends MoveEntriesValidator {

        private MoveEntriesValidationSuccess success;

        public DummyMoveEntriesValidator(ApplicationListRepository applicationListRepository) {
            super(applicationListRepository);
        }

        @Override
        public <R> R validate(
                MoveEntriesDto dto,
                java.util.function.BiFunction<MoveEntriesDto, MoveEntriesValidationSuccess, R>
                        createSupplier) {

            return createSupplier.apply(dto, success);
        }

        @Override
        public DummyMoveEntriesValidator withSourceList(UUID id) {
            return this;
        }
    }
}
