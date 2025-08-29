package uk.gov.hmcts.appregister.nationalcourthouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;
import uk.gov.hmcts.appregister.nationalcourthouse.repository.NationalCourtHouseRepository;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseImplTest {

    @Mock private NationalCourtHouseRepository repository;

    @Mock private NationalCourtHouseMapper mapper;

    @InjectMocks private NationalCourtHouseServiceImpl service;

    // -------------------- findAll --------------------

    @Test
    void findAll_mapsEntitiesToDtos_andReturnsList() {
        NationalCourtHouse entity1 = mock(NationalCourtHouse.class);
        NationalCourtHouse entity2 = mock(NationalCourtHouse.class);
        when(repository.findAll()).thenReturn(List.of(entity1, entity2));

        NationalCourtHouseDto dto1 = mock(NationalCourtHouseDto.class);
        NationalCourtHouseDto dto2 = mock(NationalCourtHouseDto.class);
        when(mapper.toReadDto(entity1)).thenReturn(dto1);
        when(mapper.toReadDto(entity2)).thenReturn(dto2);

        List<NationalCourtHouseDto> out = service.findAll();

        assertThat(out).containsExactly(dto1, dto2);
        verify(repository).findAll();
        verify(mapper).toReadDto(entity1);
        verify(mapper).toReadDto(entity2);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findAll_whenEmpty_returnsEmpty_andDoesNotCallMapper() {
        when(repository.findAll()).thenReturn(List.of());

        List<NationalCourtHouseDto> out = service.findAll();

        assertThat(out).isEmpty();
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    // -------------------- findById --------------------

    @Test
    void findById_whenFound_mapsAndReturnsDto() {
        Long id = 42L;
        NationalCourtHouse entity = mock(NationalCourtHouse.class);
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(dto);

        NationalCourtHouseDto out = service.findById(id);

        assertThat(out).isSameAs(dto);
        verify(repository).findById(id);
        verify(mapper).toReadDto(entity);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findById_whenMissing_throws404_andDoesNotCallMapper() {
        Long id = 404L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.findById(id));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("CourtLocation not found");
        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }

    // -------------------- search (name / courtType only) --------------------

    @Test
    void search_withBothFilters_buildsNonNullSpec_andMapsPage() {
        String name = "man";
        String courtType = "CROWN";
        Pageable pageable = PageRequest.of(1, 5);

        NationalCourtHouse entity = mock(NationalCourtHouse.class);
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(entity), pageable, 17);

        when(repository.findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<NationalCourtHouseDto> out =
                service.searchCourtLocations(name, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(17);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<NationalCourtHouse>> specCaptor =
                ArgumentCaptor.forClass(
                        (Class<Specification<NationalCourtHouse>>) (Class<?>) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withNameOnly_buildsNonNullSpec_andRespectsPageable() {
        String name = "card";
        Pageable pageable = PageRequest.of(0, 10);

        NationalCourtHouse entity = mock(NationalCourtHouse.class);
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<NationalCourtHouseDto> out =
                service.searchCourtLocations(name, null, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<NationalCourtHouse>> specCaptor =
                ArgumentCaptor.forClass(
                        (Class<Specification<NationalCourtHouse>>) (Class<?>) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withCourtTypeOnly_buildsNonNullSpec_andMaps() {
        String courtType = "MAGISTRATES";
        Pageable pageable = PageRequest.of(2, 3);

        NationalCourtHouse entity = mock(NationalCourtHouse.class);
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(entity), pageable, 9);

        when(repository.findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<NationalCourtHouseDto> out =
                service.searchCourtLocations(null, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(9);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<NationalCourtHouse>> specCaptor =
                ArgumentCaptor.forClass(
                        (Class<Specification<NationalCourtHouse>>) (Class<?>) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withNoFilters_stillPassesSpec_andReturnsEmptyMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable)))
                .thenReturn(repoPage);

        Page<NationalCourtHouseDto> out =
                service.searchCourtLocations(null, "   ", null, null, null, null, pageable);

        assertThat(out.getContent()).isEmpty();
        assertThat(out.getTotalElements()).isEqualTo(0);

        verify(repository)
                .findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable));
        verifyNoInteractions(mapper);
    }

    // -------------------- search (date filters) --------------------

    @Test
    void search_withDateBounds_buildsCompositeSpec_andMaps() {
        Pageable pageable = PageRequest.of(1, 20);

        // Use descriptive names to satisfy Checkstyle's LocalVariableName rule.
        LocalDate startDateFrom = LocalDate.of(2020, 1, 1);
        LocalDate startDateTo = LocalDate.of(2021, 12, 31);
        LocalDate endDateFrom = LocalDate.of(2022, 1, 1);
        LocalDate endDateTo = LocalDate.of(2024, 12, 31);

        NationalCourtHouse entity = mock(NationalCourtHouse.class);
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouse> repoPage = new PageImpl<>(List.of(entity), pageable, 3);

        when(repository.findAll(ArgumentMatchers.<Specification<NationalCourtHouse>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<NationalCourtHouseDto> out =
                service.searchCourtLocations(
                        null, null, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(21);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<NationalCourtHouse>> specCaptor =
                ArgumentCaptor.forClass(
                        (Class<Specification<NationalCourtHouse>>) (Class<?>) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }
}
