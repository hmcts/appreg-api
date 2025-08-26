package uk.gov.hmcts.appregister.courtlocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import uk.gov.hmcts.appregister.courtlocation.dto.CourtHouseDto;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtHouseMapper;
import uk.gov.hmcts.appregister.courtlocation.model.CourtHouse;
import uk.gov.hmcts.appregister.courtlocation.repository.CourtHouseRepository;

@ExtendWith(MockitoExtension.class)
class CourtLocationServiceImplTest {

    @Mock private CourtHouseRepository repository;
    @Mock private CourtHouseMapper mapper;

    @InjectMocks private CourtLocationServiceImpl service;

    @Test
    void findAll_mapsEachEntityToDto_andReturnsList() {
        // Arrange
        CourtHouse e1 = new CourtHouse(); e1.setId(1L);
        CourtHouse e2 = new CourtHouse(); e2.setId(2L);
        when(repository.findAll()).thenReturn(List.of(e1, e2));

        CourtHouseDto d1 = mock(CourtHouseDto.class);
        CourtHouseDto d2 = mock(CourtHouseDto.class);
        when(mapper.toReadDto(e1)).thenReturn(d1);
        when(mapper.toReadDto(e2)).thenReturn(d2);

        // Act
        List<CourtHouseDto> out = service.findAll();

        // Assert
        assertThat(out).containsExactly(d1, d2);
        verify(repository).findAll();
        verify(mapper).toReadDto(e1);
        verify(mapper).toReadDto(e2);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findAll_whenRepositoryEmpty_returnsEmptyList_andDoesNotCallMapper() {
        when(repository.findAll()).thenReturn(List.of());

        List<CourtHouseDto> out = service.findAll();

        assertThat(out).isEmpty();
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    @Test
    void findById_whenFound_mapsAndReturnsDto() {
        Long id = 123L;
        CourtHouse entity = new CourtHouse(); entity.setId(id);
        CourtHouseDto dto = mock(CourtHouseDto.class);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(dto);

        CourtHouseDto out = service.findById(id);

        assertThat(out).isSameAs(dto);
        verify(repository).findById(id);
        verify(mapper).toReadDto(entity);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void findById_whenMissing_throws404WithMessage_andDoesNotCallMapper() {
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex =
            assertThrows(ResponseStatusException.class, () -> service.findById(id));

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(ex.getReason()).isEqualTo("Courthouse not found");

        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }
}
