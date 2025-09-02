package uk.gov.hmcts.appregister.nationalcourthouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;
import uk.gov.hmcts.appregister.nationalcourthouse.repository.NationalCourtHouseRepository;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseServiceImplTest {

    @Mock private NationalCourtHouseRepository repository;

    @Mock private NationalCourtHouseMapper mapper;

    @InjectMocks private NationalCourtHouseServiceImpl service;

    // ---------- findAll ----------

    @Test
    void findAll_sortsByNameAscending_flattensEmptyOptionals() {
        // Arrange: repository returns two entities; mapper maps first -> DTO, second -> empty
        // Optional
        NationalCourtHouse e1 = new NationalCourtHouse();
        e1.setId(1L);
        e1.setName("Alpha");
        NationalCourtHouse e2 = new NationalCourtHouse();
        e2.setId(2L);
        e2.setName("Beta");

        when(repository.findAll(Sort.by("name").ascending())).thenReturn(List.of(e1, e2));

        NationalCourtHouseDto d1 =
                new NationalCourtHouseDto(
                        1L, "Alpha", "CROWN", LocalDate.now(), null, null, null, null, null, null);

        when(mapper.toReadDto(e1)).thenReturn(Optional.of(d1));
        when(mapper.toReadDto(e2)).thenReturn(Optional.empty()); // simulate declined mapping

        // Act
        List<NationalCourtHouseDto> out = service.findAll();

        // Assert: only the mapped DTO remains (empty optional flattened out)
        assertThat(out).containsExactly(d1);

        // Also verify we specifically asked repository for Sort.by("name").ascending()
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        Sort usedSort = sortCaptor.getValue();
        assertThat(usedSort.getOrderFor("name")).isNotNull();
        assertThat(usedSort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAll_whenRepositoryReturnsEmpty_returnsEmptyList() {
        when(repository.findAll(Sort.by("name").ascending())).thenReturn(List.of());

        List<NationalCourtHouseDto> out = service.findAll();

        assertThat(out).isEmpty();
        verify(repository).findAll(Sort.by("name").ascending());
        verifyNoInteractions(mapper);
    }

    // ---------- findById ----------

    @Test
    void findById_whenFound_mapsAndReturnsDto() {
        Long id = 42L;
        NationalCourtHouse entity = new NationalCourtHouse();
        entity.setId(id);
        NationalCourtHouseDto dto =
                new NationalCourtHouseDto(
                        id,
                        "Leeds",
                        "CROWN",
                        LocalDate.of(2020, 1, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(Optional.of(dto));

        NationalCourtHouseDto out = service.findById(id);

        assertThat(out).isSameAs(dto);
        verify(repository).findById(id);
        verify(mapper).toReadDto(entity);
    }

    @Test
    void findById_whenMissing_throws404() {
        Long id = 404L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.findById(id));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void findById_whenMapperDeclinesMapping_throws404() {
        // If the entity exists but mapper returns Optional.empty(), service should throw 404
        Long id = 7L;
        NationalCourtHouse entity = new NationalCourtHouse();
        entity.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.findById(id));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository).findById(id);
        verify(mapper).toReadDto(entity);
    }

    // ---------- search ----------

    @Test
    void search_withFilters_delegatesToRepository_andMapsEntities() {
        String name = "card";
        String courtType = "CROWN";
        LocalDate startDateFrom = LocalDate.of(2020, 1, 1);
        LocalDate startDateTo = LocalDate.of(2020, 12, 31);
        LocalDate endDateFrom = LocalDate.of(2021, 1, 1);
        LocalDate endDateTo = LocalDate.of(2022, 1, 1);
        Pageable pageable = PageRequest.of(1, 5);

        // Repository returns a page of entities
        NationalCourtHouse e1 = new NationalCourtHouse();
        e1.setId(10L);
        NationalCourtHouse e2 = new NationalCourtHouse();
        e2.setId(20L);

        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(e1, e2), pageable, 9);
        when(repository.search(
                        eq(name),
                        eq(courtType),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        any(Pageable.class)))
                .thenReturn(repoPage);

        // Mapper returns DTOs wrapped in Optional
        NationalCourtHouseDto d1 =
                new NationalCourtHouseDto(
                        10L,
                        "Cardiff",
                        "CROWN",
                        LocalDate.of(2019, 1, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        NationalCourtHouseDto d2 =
                new NationalCourtHouseDto(
                        20L,
                        "Cardigan",
                        "CROWN",
                        LocalDate.of(2018, 1, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        when(mapper.toReadDto(e1)).thenReturn(Optional.of(d1));
        when(mapper.toReadDto(e2)).thenReturn(Optional.of(d2));

        // Act
        Page<NationalCourtHouseDto> out =
                service.search(
                        name,
                        courtType,
                        startDateFrom,
                        startDateTo,
                        endDateFrom,
                        endDateTo,
                        pageable);

        // Assert: page content mapped, metadata preserved
        assertThat(out.getContent()).containsExactly(d1, d2);
        assertThat(out.getTotalElements()).isEqualTo(7);
        assertThat(out.getNumber()).isEqualTo(1);
        assertThat(out.getSize()).isEqualTo(5);

        // Verify repository was called with the same pageable we supplied
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository)
                .search(
                        eq(name),
                        eq(courtType),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void search_whenMapperReturnsEmpty_forAnyEntity_failsFast() {
        // Arrange: one of the entities cannot be mapped (mapper returns Optional.empty())
        Pageable pageable = PageRequest.of(0, 2);
        NationalCourtHouse e1 = new NationalCourtHouse();
        e1.setId(1L);
        NationalCourtHouse e2 = new NationalCourtHouse();
        e2.setId(2L);

        when(repository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

        when(mapper.toReadDto(e1))
                .thenReturn(
                        Optional.of(
                                new NationalCourtHouseDto(
                                        1L,
                                        "A",
                                        "CROWN",
                                        LocalDate.now(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null)));
        when(mapper.toReadDto(e2)).thenReturn(Optional.empty()); // simulate declined mapping

        // Act + Assert: service should throw IllegalStateException to surface the issue
        assertThrows(
                IllegalStateException.class,
                () -> service.search(null, null, null, null, null, null, pageable));
    }
}
