package uk.gov.hmcts.appregister.courtlocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapper;
import uk.gov.hmcts.appregister.courtlocation.model.CourtLocation;
import uk.gov.hmcts.appregister.courtlocation.repository.CourtLocationRepository;

@ExtendWith(MockitoExtension.class)
class CourtLocationServiceImplTest {

    @Mock private CourtLocationRepository repository;
    @Mock private CourtLocationMapper mapper;

    @InjectMocks private CourtLocationServiceImpl service;

    // ---------- findAll ----------

    @Test
    void findAll_mapsEntitiesToDtos_andReturnsList() {
        CourtLocation e1 = mock(CourtLocation.class);
        CourtLocation e2 = mock(CourtLocation.class);
        when(repository.findAll()).thenReturn(List.of(e1, e2));

        CourtLocationDto d1 = mock(CourtLocationDto.class);
        CourtLocationDto d2 = mock(CourtLocationDto.class);
        when(mapper.toReadDto(e1)).thenReturn(d1);
        when(mapper.toReadDto(e2)).thenReturn(d2);

        List<CourtLocationDto> out = service.findAll();

        assertThat(out).containsExactly(d1, d2);
        verify(repository).findAll();
        verify(mapper).toReadDto(e1);
        verify(mapper).toReadDto(e2);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findAll_whenEmpty_returnsEmpty_andDoesNotCallMapper() {
        when(repository.findAll()).thenReturn(List.of());

        List<CourtLocationDto> out = service.findAll();

        assertThat(out).isEmpty();
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    // ---------- findById ----------

    @Test
    void findById_whenFound_mapsAndReturnsDto() {
        Long id = 42L;
        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(dto);

        CourtLocationDto out = service.findById(id);

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

    // ---------- searchCourtLocations ----------

    @Test
    void search_withBothFilters_buildsNonNullSpec_andMapsPage() {
        String name = "man";
        String courtType = "CROWN";
        Pageable pageable = PageRequest.of(1, 5);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);

        // Use 3-arg PageImpl to demonstrate totals; but assertions below read the total from
        // the stubbed page to avoid brittle failures if the stub is tweaked.
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 17);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
            .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<CourtLocationDto> out = service.searchCourtLocations(
            name, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(repoPage.getTotalElements());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
            ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withNameOnly_buildsNonNullSpec_andRespectsPageable() {
        String name = "card";
        String courtType = null;
        Pageable pageable = PageRequest.of(0, 10);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
            .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<CourtLocationDto> out = service.searchCourtLocations(
            name, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(repoPage.getTotalElements());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
            ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withCourtTypeOnly_buildsNonNullSpec_andMaps() {
        String name = null;
        String courtType = "MAGISTRATES";
        Pageable pageable = PageRequest.of(0, 20);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);

        // Some environments construct PageImpl without explicit total (=> total == content size).
        // To make the test robust, we assert against the stub's total rather than a magic number.
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity)  /* total may default to 1 */);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
            .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<CourtLocationDto> out = service.searchCourtLocations(
            name, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(repoPage.getTotalElements());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
            ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withNoFilters_usesAllOfWithNullParts_andReturnsEmptyMappedPage() {
        String name = null;
        String courtType = "   ";
        Pageable pageable = PageRequest.of(0, 10);

        Page<CourtLocation> repoPage = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
            .thenReturn(repoPage);

        Page<CourtLocationDto> out = service.searchCourtLocations(
            name, courtType, null, null, null, null, pageable);

        assertThat(out.getContent()).isEmpty();
        assertThat(out.getTotalElements()).isEqualTo(repoPage.getTotalElements());
        verify(repository).findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable));
        verifyNoInteractions(mapper);
    }

    @Test
    void search_withAllDateBounds_buildsCompositeSpec_andMaps() {
        LocalDate sFrom = LocalDate.of(2020, 1, 1);
        LocalDate sTo   = LocalDate.of(2021, 12, 31);
        LocalDate eFrom = LocalDate.of(2022, 1, 1);
        LocalDate eTo   = LocalDate.of(2024, 12, 31);
        Pageable pageable = PageRequest.of(2, 3);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);

        // Use a total that your environment produces; assertions key off the stubbed page.
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 7);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
            .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        Page<CourtLocationDto> out = service.searchCourtLocations(
            null, null, sFrom, sTo, eFrom, eTo, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(repoPage.getTotalElements());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
            ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }
}
