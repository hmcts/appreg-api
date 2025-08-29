package uk.gov.hmcts.appregister.nationalcourthouse.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHousePageResponse;
import uk.gov.hmcts.appregister.nationalcourthouse.service.NationalCourtHouseService;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseControllerTest {

    @Mock private NationalCourtHouseService service;

    @InjectMocks private NationalCourtHouseController controller;

    @Test
    void list_defaults_applyAndServiceIsCalledWithSortedPageable() {
        // Arrange: service returns a simple page of 2 DTOs when no filters are present
        NationalCourtHouseDto d1 = mock(NationalCourtHouseDto.class);
        NationalCourtHouseDto d2 = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouseDto> page = new PageImpl<>(List.of(d1, d2), PageRequest.of(0, 10), 2);

        when(service.searchCourtLocations(
                        isNull(), // name
                        isNull(), // courtType
                        isNull(), // startDateFrom
                        isNull(), // startDateTo
                        isNull(), // endDateFrom
                        isNull(), // endDateTo
                        any(Pageable.class) // pageable
                        ))
                .thenReturn(page);

        // Act: call with all query params omitted -> defaults should kick in
        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, null, null, null, null, null, null);

        // Assert: response basics
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(d1, d2);
        assertThat(resp.getBody().totalCount()).isEqualTo(2L);
        assertThat(resp.getBody().page()).isEqualTo(1);
        assertThat(resp.getBody().pageSize()).isEqualTo(10);

        // Capture the Pageable the controller passed down and assert pagination/sort semantics
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .searchCourtLocations(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0); // controller converts 1 -> 0
        assertThat(used.getPageSize()).isEqualTo(10);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withFiltersAndPaging_passedThroughAndReflectedInResponse() {
        // Arrange: set up filters & paging (note: page is 1-based in API)
        String name = "card";
        String courtType = "CROWN";
        Integer pageParam = 2;
        Integer sizeParam = 5;

        // Date filters
        LocalDate startDateFrom = LocalDate.of(2020, 1, 1);
        LocalDate startDateTo = LocalDate.of(2021, 12, 31);
        LocalDate endDateFrom = LocalDate.of(2022, 1, 1);
        LocalDate endDateTo = LocalDate.of(2024, 12, 31);

        NationalCourtHouseDto d1 = mock(NationalCourtHouseDto.class);
        Page<NationalCourtHouseDto> page = new PageImpl<>(List.of(d1), PageRequest.of(1, 5), 21);

        when(service.searchCourtLocations(
                        eq(name),
                        eq(courtType),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(
                        name,
                        courtType,
                        pageParam,
                        sizeParam,
                        startDateFrom,
                        startDateTo,
                        endDateFrom,
                        endDateTo);

        // Assert: response payload mirrors the Page plus requested page metadata
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(d1);
        assertThat(resp.getBody().totalCount()).isEqualTo(21L);
        assertThat(resp.getBody().page()).isEqualTo(2);
        assertThat(resp.getBody().pageSize()).isEqualTo(5);

        // Verify pageable was translated correctly (1-based -> 0-based) and sorted by name ASC
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .searchCourtLocations(
                        eq(name),
                        eq(courtType),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_whenPageLessThanOne_returnsBadRequest_andDoesNotCallService() {
        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, 0, 10, null, null, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenPageSizeLessThanOne_returnsBadRequest_andDoesNotCallService() {
        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, 1, 0, null, null, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenPageSizeOverMax_returnsBadRequest_andDoesNotCallService() {
        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, 1, 101, null, null, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenInvalidStartRange_returnsBadRequest() {
        // startDateFrom is after startDateTo -> 400
        LocalDate startDateFrom = LocalDate.of(2024, 2, 1);
        LocalDate startDateTo = LocalDate.of(2024, 1, 1);

        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, 1, 10, startDateFrom, startDateTo, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenInvalidEndRange_returnsBadRequest() {
        // endDateFrom is after endDateTo -> 400
        LocalDate endDateFrom = LocalDate.of(2025, 1, 2);
        LocalDate endDateTo = LocalDate.of(2025, 1, 1);

        ResponseEntity<NationalCourtHousePageResponse> resp =
                controller.list(null, null, 1, 10, null, null, endDateFrom, endDateTo);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void getById_returnsOkWithBody() {
        Long id = 123L;
        NationalCourtHouseDto dto = mock(NationalCourtHouseDto.class);
        when(service.findById(id)).thenReturn(dto);

        ResponseEntity<NationalCourtHouseDto> resp = controller.getById(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
        verify(service).findById(id);
    }

    @Test
    void getById_whenServiceThrows_propagatesException() {
        Long id = 404L;
        when(service.findById(id))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.getById(id));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("not found");
        verify(service).findById(id);
    }
}
