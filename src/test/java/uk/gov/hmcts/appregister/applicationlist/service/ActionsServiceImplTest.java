package uk.gov.hmcts.appregister.applicationlist.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.CLOSED;
import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.OPEN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@ExtendWith(MockitoExtension.class)
public class ActionsServiceImplTest {

    @Mock private ApplicationListRepository alRepository;
    @Mock private ApplicationListEntryRepository aleRepository;

    private ActionsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActionsServiceImpl(alRepository, aleRepository);
    }

    @Captor private ArgumentCaptor<List<ApplicationListEntry>> listCaptor;

    @Test
    void move() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        Set<UUID> requestedIds = new HashSet<>();

        ApplicationListEntry entry1 = new ApplicationListEntry();
        entry1.setUuid(UUID.randomUUID());
        entry1.setApplicationList(sourceList);
        requestedIds.add(entry1.getUuid());

        ApplicationListEntry entry2 = new ApplicationListEntry();
        entry2.setUuid(UUID.randomUUID());
        entry2.setApplicationList(sourceList);
        requestedIds.add(entry2.getUuid());

        List<ApplicationListEntry> applicationListEntries = new ArrayList<>();
        applicationListEntries.add(entry1);
        applicationListEntries.add(entry2);

        dto.setEntryIds(requestedIds);

        when(aleRepository.findAllByUuidIn(dto.getEntryIds())).thenReturn(applicationListEntries);

        service.move(sourceListId, dto);

        verify(alRepository).findByUuid(sourceListId);
        verify(alRepository).findByUuid(targetListId);
        verify(aleRepository).findAllByUuidIn(dto.getEntryIds());

        verify(aleRepository).saveAll(listCaptor.capture());
        List<ApplicationListEntry> capturedList = listCaptor.getValue();

        Assertions.assertEquals(2, capturedList.size());
        Assertions.assertTrue(capturedList.contains(entry1));
        Assertions.assertTrue(capturedList.contains(entry2));
    }

    @Test
    void move_returns404_whenSourceListDoesNotExist() {
        UUID sourceListId = UUID.randomUUID();
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns404_whenTargetListDoesNotExist() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        UUID targetListId = UUID.randomUUID();
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenSourceListNotOpen() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(CLOSED);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenTargetListNotOpen() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(CLOSED);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsNull() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsEmpty() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(new HashSet<>());

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns404_whenEntryDoesNotExist() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        Set<UUID> requestedIds = new HashSet<>();

        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setApplicationList(sourceList);
        requestedIds.add(entry.getUuid());

        dto.setEntryIds(requestedIds);

        when(aleRepository.findAllByUuidIn(dto.getEntryIds())).thenReturn(emptyList());

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenEntryNotInSourceList() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        Set<UUID> requestedIds = new HashSet<>();

        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setApplicationList(new ApplicationList());
        requestedIds.add(entry.getUuid());

        List<ApplicationListEntry> applicationListEntries = new ArrayList<>();
        applicationListEntries.add(entry);

        dto.setEntryIds(requestedIds);

        when(aleRepository.findAllByUuidIn(dto.getEntryIds())).thenReturn(applicationListEntries);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryAlreadyInTargetList() {
        ApplicationList sourceList = new ApplicationList();
        UUID sourceListId = UUID.randomUUID();
        sourceList.setUuid(sourceListId);
        sourceList.setStatus(OPEN);
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(sourceList));

        ApplicationList targetList = new ApplicationList();
        UUID targetListId = UUID.randomUUID();
        targetList.setUuid(targetListId);
        targetList.setStatus(OPEN);
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(targetList));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        Set<UUID> requestedIds = new HashSet<>();

        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setApplicationList(targetList);
        requestedIds.add(entry.getUuid());

        List<ApplicationListEntry> applicationListEntries = new ArrayList<>();
        applicationListEntries.add(entry);

        dto.setEntryIds(requestedIds);

        when(aleRepository.findAllByUuidIn(dto.getEntryIds())).thenReturn(applicationListEntries);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
