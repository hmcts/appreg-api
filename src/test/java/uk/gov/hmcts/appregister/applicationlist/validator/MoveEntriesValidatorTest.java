package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.CLOSED;
import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.OPEN;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@ExtendWith(MockitoExtension.class)
public class MoveEntriesValidatorTest {

    @Mock private ApplicationListRepository alRepository;
    @Mock private ApplicationListEntryRepository aleRepository;

    @InjectMocks private MoveEntriesValidator validator;

    private UUID sourceListId;
    private UUID targetListId;

    @BeforeEach
    void setUp() {
        sourceListId = UUID.randomUUID();
        targetListId = UUID.randomUUID();
    }

    @Test
    void validate_successful_whenValidRequest() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        ApplicationListEntry e1 = new ApplicationListEntry();
        e1.setUuid(UUID.randomUUID());
        e1.setApplicationList(source);

        ApplicationListEntry e2 = new ApplicationListEntry();
        e2.setUuid(UUID.randomUUID());
        e2.setApplicationList(source);

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(e1.getUuid(), e2.getUuid()));

        when(aleRepository.findAllByUuidInAndApplicationListUuid(dto.getEntryIds(), sourceListId))
                .thenReturn(List.of(e1, e2));

        MoveEntriesValidationSuccess success =
                validator.withSourceList(sourceListId).validate(dto, (d, s) -> s);

        Assertions.assertNotNull(success);
        Assertions.assertEquals(target, success.getTargetList());
        Assertions.assertEquals(2, success.getEntriesToSave().size());
        success.getEntriesToSave()
                .forEach(
                        e ->
                                Assertions.assertEquals(
                                        target.getUuid(), e.getApplicationList().getUuid()));
    }

    @Test
    void validate_throws_invalidListStatus_whenSourceListNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(CLOSED); // not open

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("source list")
                .hasMessageContaining(sourceListId.toString());
    }

    @Test
    void validate_throws_invalidListStatus_whenTargetListNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(CLOSED); // not open

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("target list")
                .hasMessageContaining(targetListId.toString());
    }

    @Test
    void validate_throws_invalidListStatus_whenBothListsNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(CLOSED); // not open

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(CLOSED); // not open

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("source list")
                .hasMessageContaining("target list")
                .hasMessageContaining(sourceListId.toString())
                .hasMessageContaining(targetListId.toString());
    }

    @Test
    void validate_throws_notFound_whenSourceListMissing() {
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_notFound_whenTargetListMissing() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.TARGET_LIST_NOT_FOUND,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_entryNotProvided_whenEntryIdsNullOrEmpty() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dtoNull = new MoveEntriesDto();
        dtoNull.setTargetListId(targetListId);
        dtoNull.setEntryIds(null);

        assertThatThrownBy(
                        () -> validator.withSourceList(sourceListId).validate(dtoNull, (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_PROVIDED,
                                        ((AppRegistryException) ex).getCode()));

        MoveEntriesDto dtoEmpty = new MoveEntriesDto();
        dtoEmpty.setTargetListId(targetListId);
        dtoEmpty.setEntryIds(Set.of());

        assertThatThrownBy(
                        () ->
                                validator
                                        .withSourceList(sourceListId)
                                        .validate(dtoEmpty, (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_PROVIDED,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_entryNotInSourceList_whenEntryMissing() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        UUID missingId = UUID.randomUUID();
        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(missingId));

        // single filtered call returns empty -> fast-fail as "not in source list"
        when(aleRepository.findAllByUuidInAndApplicationListUuid(dto.getEntryIds(), sourceListId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_entryNotInSourceList_whenEntryNotFromSource() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        ApplicationListEntry wrongEntry = new ApplicationListEntry();
        wrongEntry.setUuid(UUID.randomUUID());
        wrongEntry.setApplicationList(new ApplicationList()); // wrong list

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(wrongEntry.getUuid()));

        // Because the repo call filters to sourceListId, wrongEntry will NOT be returned
        when(aleRepository.findAllByUuidInAndApplicationListUuid(dto.getEntryIds(), sourceListId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> validator.withSourceList(sourceListId).validate(dto, (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                        ((AppRegistryException) ex).getCode()));
    }
}
