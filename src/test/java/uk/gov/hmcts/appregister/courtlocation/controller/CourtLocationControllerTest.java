package uk.gov.hmcts.appregister.courtlocation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationPageResponse;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;

@ExtendWith(MockitoExtension.class)
class CourtLocationControllerTest {

    @Mock
    private CourtLocationService service;

    @InjectMocks
    private CourtLocationController controller;

    @Test
    void list_whenNoParams_appliesDefaults_andCallsServiceWithNameSortedPageable() {
        // Arrange: a page with 2 DTOs, created using 0-based PageRequest(0,10)
        CourtLocationDto d1 = new CourtLocationDto(1L, "A", null, null, null, null, null, null, null, null);
        CourtLocationDto d2 = new CourtLocationDto(2L, "B", null, null, null, null, null, null, null, null);
        Page<CourtLocationDto> svcPage = new PageImpl<>(List.of(d1, d2), PageRequest.of(0, 10), 2);

        // Use lenient "any(...)" matchers for stubbing to avoid strict argument-mismatch
        // while still capturing exact values in verification below.
        when(service.searchCourtLocations(
            any(), any(), any(), any(), any(), any(), any(Pageable.class))
        ).thenReturn(svcPage);

        // Act: call with all nulls (controller should apply defaults page=1,size=10)
        ResponseEntity<CourtLocationPageResponse> resp =
            controller.list(null, null, null, null, null, null, null, null);

        // Assert: successful response with expected body and paging metadata
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        CourtLocationPageResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.results()).containsExactly(d1, d2);
        assertThat(body.totalCount()).isEqualTo(2L);
        assertThat(body.page()).isEqualTo(1);       // API is 1-based
        assertThat(body.pageSize()).isEqualTo(10);

        // Capture the exact arguments passed to the service to assert defaults were propagated
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> courtTypeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> sFromCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> sToCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> eFromCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> eToCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);

        // Verify a single invocation and capture arguments in order
        org.mockito.Mockito.verify(service).searchCourtLocations(
            nameCap.capture(),
            courtTypeCap.capture(),
            sFromCap.capture(),
            sToCap.capture(),
            eFromCap.capture(),
            eToCap.capture(),
            pageableCap.capture()
        );

        // All filters should be null when not provided
        assertThat(nameCap.getValue()).isNull();
        assertThat(courtTypeCap.getValue()).isNull();
        assertThat(sFromCap.getValue()).isNull();
        assertThat(sToCap.getValue()).isNull();
        assertThat(eFromCap.getValue()).isNull();
        assertThat(eToCap.getValue()).isNull();

        // Pageable should be 0-based (page 0) with size 10 and sorted by name ASC
        Pageable used = pageableCap.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(10);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withAllFiltersAndPaging_passesThroughFilters_andBuildsResponse() {
        // Arrange: explicit filters and paging
        String name = "card";
        String courtType = "CROWN";
        Integer page = 2;    // API 1-based => should map to 0-based page 1
        Integer size = 5;
        LocalDate sFrom = LocalDate.of(2020, 1, 1);
        LocalDate sTo = LocalDate.of(2021, 1, 1);
        LocalDate eFrom = LocalDate.of(2022, 1, 1);
        LocalDate eTo = LocalDate.of(2023, 1, 1);

        CourtLocationDto d = new CourtLocationDto(10L, "Cardiff", "CROWN", null, null, null, null, null, null, null);
        Page<CourtLocationDto> svcPage = new PageImpl<>(List.of(d), PageRequest.of(1, 5), 21);

        when(service.searchCourtLocations(
            any(), any(), any(), any(), any(), any(), any(Pageable.class))
        ).thenReturn(svcPage);

        // Act
        ResponseEntity<CourtLocationPageResponse> resp =
            controller.list(name, courtType, page, size, sFrom, sTo, eFrom, eTo);

        // Assert: body reflects the Page from service, but with 1-based page
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        CourtLocationPageResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.results()).containsExactly(d);
        assertThat(body.totalCount()).isEqualTo(21L);
        assertThat(body.page()).isEqualTo(2);
        assertThat(body.pageSize()).isEqualTo(5);

        // Verify exact args passed to service
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> courtTypeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> sFromCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> sToCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> eFromCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> eToCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);

        org.mockito.Mockito.verify(service).searchCourtLocations(
            nameCap.capture(),
            courtTypeCap.capture(),
            sFromCap.capture(),
            sToCap.capture(),
            eFromCap.capture(),
            eToCap.capture(),
            pageableCap.capture()
        );

        assertThat(nameCap.getValue()).isEqualTo(name);
        assertThat(courtTypeCap.getValue()).isEqualTo(courtType);
        assertThat(sFromCap.getValue()).isEqualTo(sFrom);
        assertThat(sToCap.getValue()).isEqualTo(sTo);
        assertThat(eFromCap.getValue()).isEqualTo(eFrom);
        assertThat(eToCap.getValue()).isEqualTo(eTo);

        Pageable used = pageableCap.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1); // converted to 0-based
        assertThat(used.getPageSize()).isEqualTo(5);
        Sort.Order nameOrder = used.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_whenPageLessThanOne_returns400_andDoesNotCallService() {
        ResponseEntity<CourtLocationPageResponse> resp =
            controller.list(null, null, 0, 10, null, null, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service); // ensure short-circuit before service call
    }

    @Test
    void list_whenPageSizeInvalid_returns400_andDoesNotCallService() {
        // size < 1
        ResponseEntity<CourtLocationPageResponse> r1 =
            controller.list(null, null, 1, 0, null, null, null, null);
        assertThat(r1.getStatusCodeValue()).isEqualTo(400);

        // size > MAX (100)
        ResponseEntity<CourtLocationPageResponse> r2 =
            controller.list(null, null, 1, 101, null, null, null, null);
        assertThat(r2.getStatusCodeValue()).isEqualTo(400);

        verifyNoInteractions(service);
    }

    @Test
    void list_whenStartDateRangeInvalid_returns400_andDoesNotCallService() {
        LocalDate from = LocalDate.of(2024, 1, 2);
        LocalDate to = LocalDate.of(2024, 1, 1); // from > to -> invalid
        ResponseEntity<CourtLocationPageResponse> resp =
            controller.list(null, null, 1, 10, from, to, null, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenEndDateRangeInvalid_returns400_andDoesNotCallService() {
        LocalDate from = LocalDate.of(2024, 1, 2);
        LocalDate to = LocalDate.of(2024, 1, 1); // from > to -> invalid
        ResponseEntity<CourtLocationPageResponse> resp =
            controller.list(null, null, 1, 10, null, null, from, to);

        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void getById_whenServiceReturnsDto_returnsOkWithBody() {
        Long id = 123L;
        CourtLocationDto dto = new CourtLocationDto(id, "X", null, null, null, null, null, null, null, null);
        when(service.findById(id)).thenReturn(dto);

        ResponseEntity<CourtLocationDto> resp = controller.getById(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void getById_whenServiceThrows404_isPropagated() {
        Long id = 404L;
        when(service.findById(id))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getById(id));
        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("not found");
    }
}
