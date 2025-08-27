package uk.gov.hmcts.appregister.courtlocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
        // Arrange: repository returns two entities
        CourtLocation e1 = mock(CourtLocation.class);
        CourtLocation e2 = mock(CourtLocation.class);
        when(repository.findAll()).thenReturn(List.of(e1, e2));

        // Map each entity to a DTO
        CourtLocationDto d1 = mock(CourtLocationDto.class);
        CourtLocationDto d2 = mock(CourtLocationDto.class);
        when(mapper.toReadDto(e1)).thenReturn(d1);
        when(mapper.toReadDto(e2)).thenReturn(d2);

        // Act: call the service
        List<CourtLocationDto> out = service.findAll();

        // Assert: both entities were mapped and returned in order
        assertThat(out).containsExactly(d1, d2);
        verify(repository).findAll();
        verify(mapper).toReadDto(e1);
        verify(mapper).toReadDto(e2);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findAll_whenEmpty_returnsEmpty_andDoesNotCallMapper() {
        // Arrange: repository has no records
        when(repository.findAll()).thenReturn(List.of());

        // Act
        List<CourtLocationDto> out = service.findAll();

        // Assert: result is empty and mapper was never used
        assertThat(out).isEmpty();
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    // ---------- findById ----------

    @Test
    void findById_whenFound_mapsAndReturnsDto() {
        // Arrange: repository finds entity
        Long id = 42L;
        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(dto);

        // Act
        CourtLocationDto out = service.findById(id);

        // Assert: correct entity was mapped and returned
        assertThat(out).isSameAs(dto);
        verify(repository).findById(id);
        verify(mapper).toReadDto(entity);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findById_whenMissing_throws404_andDoesNotCallMapper() {
        // Arrange: repository returns empty
        Long id = 404L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert: service should throw 404
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.findById(id));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("CourtLocation not found");

        // Ensure mapper never called
        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }

    // ---------- searchCourtLocations ----------

    @Test
    void search_withBothFilters_buildsNonNullSpec_andMapsPage() {
        // Arrange: name + type filters, repository returns one entity in a Page
        String name = "man";
        String courtType = "CROWN";
        Pageable pageable = PageRequest.of(1, 5);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 17);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        // Act
        Page<CourtLocationDto> out = service.searchCourtLocations(name, courtType, pageable);

        // Assert: mapped DTO present and total count preserved
        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(17);

        // Verify spec built and passed to repository
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        Specification<CourtLocation> used = specCaptor.getValue();
        assertThat(used).isNotNull();
    }

    @Test
    void search_withNameOnly_buildsNonNullSpec_andRespectsPageable() {
        // Arrange: only name filter supplied
        String name = "card";
        String courtType = null;
        Pageable pageable = PageRequest.of(0, 10);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        // Act
        Page<CourtLocationDto> out = service.searchCourtLocations(name, courtType, pageable);

        // Assert: result contains mapped DTO
        assertThat(out.getContent()).containsExactly(dto);

        // Verify non-null spec was passed
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        Specification<CourtLocation> used = specCaptor.getValue();
        assertThat(used).isNotNull();
    }

    @Test
    void search_withCourtTypeOnly_buildsNonNullSpec() {
        // Arrange: only courtType filter supplied
        String name = null;
        String courtType = "MAGISTRATES";
        Pageable pageable = PageRequest.of(2, 3);

        CourtLocation entity = mock(CourtLocation.class);
        CourtLocationDto dto = mock(CourtLocationDto.class);
        Page<CourtLocation> repoPage = new PageImpl<>(List.of(entity), pageable, 9);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
                .thenReturn(repoPage);
        when(mapper.toReadDto(entity)).thenReturn(dto);

        // Act
        Page<CourtLocationDto> out = service.searchCourtLocations(name, courtType, pageable);

        // Assert
        assertThat(out.getContent()).containsExactly(dto);

        // Verify non-null spec was passed
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<CourtLocation>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), eq(pageable));
        Specification<CourtLocation> used = specCaptor.getValue();
        assertThat(used).isNotNull();
    }

    @Test
    void search_withNoFilters_usesMatchAllSpec_andReturnsEmptyMappedPage() {
        // Arrange: no filters (blank type treated as no filter)
        String name = null;
        String courtType = "   ";
        Pageable pageable = PageRequest.of(0, 10);

        Page<CourtLocation> repoPage = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable)))
                .thenReturn(repoPage);

        // Act
        Page<CourtLocationDto> out = service.searchCourtLocations(name, courtType, pageable);

        // Assert: empty page returned
        assertThat(out.getContent()).isEmpty();
        assertThat(out.getTotalElements()).isEqualTo(0);

        // Verify that repository was still called with a spec (match-all) and pageable
        verify(repository)
                .findAll(ArgumentMatchers.<Specification<CourtLocation>>any(), eq(pageable));
        // Mapper should not be called since no entities returned
        verifyNoInteractions(mapper);
    }
}
