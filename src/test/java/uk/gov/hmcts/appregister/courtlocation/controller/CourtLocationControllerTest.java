package uk.gov.hmcts.appregister.courtlocation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationPageResponse;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;

@ExtendWith(MockitoExtension.class)
class CourtLocationControllerTest {

    @Mock private CourtLocationService service;
    @InjectMocks private CourtLocationController controller;

    @Test
    void list_defaults_applyAndServiceIsCalledWithSortedPageable() {
        // Arrange: mock two DTOs and a Page result
        CourtLocationDto d1 = mock(CourtLocationDto.class);
        CourtLocationDto d2 = mock(CourtLocationDto.class);
        Page<CourtLocationDto> page = new PageImpl<>(List.of(d1, d2), PageRequest.of(0, 10), 2);

        // Stub service to return the page when no filters and any Pageable are passed
        when(service.searchCourtLocations(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act: call list with all params null (defaults should apply)
        ResponseEntity<CourtLocationPageResponse> resp = controller.list(null, null, null, null);

        // Assert: response is OK and contains expected data/metadata
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(d1, d2);
        assertThat(resp.getBody().totalCount()).isEqualTo(2L);
        assertThat(resp.getBody().page()).isEqualTo(1); // public API uses 1-based pages
        assertThat(resp.getBody().pageSize()).isEqualTo(10);

        // Capture the Pageable passed into the service and assert it has correct defaults
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).searchCourtLocations(isNull(), isNull(), captor.capture());

        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0); // internally 0-based
        assertThat(used.getPageSize()).isEqualTo(10);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withFiltersAndPaging_passedThroughAndReflectedInResponse() {
        // Arrange: set filters and custom page params
        String name = "card";
        String courtType = "CROWN";
        Integer pageParam = 2;
        Integer sizeParam = 5;

        // Prepare one DTO and a Page result with total count 21
        CourtLocationDto d1 = mock(CourtLocationDto.class);
        Page<CourtLocationDto> page = new PageImpl<>(List.of(d1), PageRequest.of(1, 5), 21);

        // Stub service with expected filter values and Pageable
        when(service.searchCourtLocations(eq(name), eq(courtType), any(Pageable.class)))
                .thenReturn(page);

        // Act: call list with filters and pagination
        ResponseEntity<CourtLocationPageResponse> resp =
                controller.list(name, courtType, pageParam, sizeParam);

        // Assert: response contains expected values
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(d1);
        assertThat(resp.getBody().totalCount()).isEqualTo(21L);
        assertThat(resp.getBody().page()).isEqualTo(2);
        assertThat(resp.getBody().pageSize()).isEqualTo(5);

        // Verify Pageable used matches the requested params
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).searchCourtLocations(eq(name), eq(courtType), captor.capture());

        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1); // page=2 externally -> page=1 internally
        assertThat(used.getPageSize()).isEqualTo(5);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_whenPageLessThanOne_returnsBadRequest_andDoesNotCallService() {
        // Invalid page param should return 400 and not hit the service
        ResponseEntity<CourtLocationPageResponse> resp = controller.list(null, null, 0, 10);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenPageSizeLessThanOne_returnsBadRequest_andDoesNotCallService() {
        // Invalid pageSize param (zero) should return 400
        ResponseEntity<CourtLocationPageResponse> resp = controller.list(null, null, 1, 0);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenPageSizeOverMax_returnsBadRequest_andDoesNotCallService() {
        // Invalid pageSize param (>100) should return 400
        ResponseEntity<CourtLocationPageResponse> resp = controller.list(null, null, 1, 101);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void getById_returnsOkWithBody() {
        // Arrange: stub service to return a DTO for given ID
        Long id = 123L;
        CourtLocationDto dto = mock(CourtLocationDto.class);
        when(service.findById(id)).thenReturn(dto);

        // Act: call getById
        ResponseEntity<CourtLocationDto> resp = controller.getById(id);

        // Assert: service was called, response is OK and body is the same DTO
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
        verify(service).findById(id);
    }

    @Test
    void getById_whenServiceThrows_propagatesException() {
        // Arrange: stub service to throw 404 ResponseStatusException
        Long id = 404L;
        when(service.findById(id))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        // Act + Assert: controller should propagate the exception unchanged
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.getById(id));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("not found");
        verify(service).findById(id);
    }
}
