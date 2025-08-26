package uk.gov.hmcts.appregister.courtlocation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtHouseDto;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;

@ExtendWith(MockitoExtension.class)
class CourtHouseControllerTest {

    @Mock private CourtLocationService service;

    @InjectMocks private CourtHouseController controller;

    @Test
    void getAll_returnsOkWithBody() {
        CourtHouseDto dto1 = mock(CourtHouseDto.class);
        CourtHouseDto dto2 = mock(CourtHouseDto.class);
        when(service.findAll()).thenReturn(List.of(dto1, dto2));

        ResponseEntity<List<CourtHouseDto>> resp = controller.getAll();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(dto1, dto2);
        verify(service).findAll();
    }

    @Test
    void getById_returnsOkWithBody() {
        CourtHouseDto dto = mock(CourtHouseDto.class);
        when(service.findById(123L)).thenReturn(dto);

        ResponseEntity<CourtHouseDto> resp = controller.getById(123L);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
        verify(service).findById(123L);
    }
}
