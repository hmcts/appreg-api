package uk.gov.hmcts.appregister.resolutioncode.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodePageResponse;
import uk.gov.hmcts.appregister.resolutioncode.service.ResolutionCodeService;

@ExtendWith(MockitoExtension.class)
class ResolutionCodeControllerTest {

    @Mock private ResolutionCodeService service;

    @InjectMocks private ResolutionCodeController controller;

    @Test
    void list_defaults_applyAndServiceCalledWithTitleAscSort() {
        // Arrange: mock a page with two DTO list items and default 0-based page request
        ResolutionCodeListItemDto i1 = org.mockito.Mockito.mock(ResolutionCodeListItemDto.class);
        ResolutionCodeListItemDto i2 = org.mockito.Mockito.mock(ResolutionCodeListItemDto.class);
        Page<ResolutionCodeListItemDto> page =
                new PageImpl<>(List.of(i1, i2), PageRequest.of(0, 10), 2);

        // When no filters/page params are provided, controller should pass nulls and a pageable
        when(service.search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(page);

        // Act: call with all nulls (defaults should kick in: page=1, size=10)
        ResponseEntity<ResolutionCodePageResponse> resp =
                controller.list(null, null, null, null, null, null, null, null);

        // Assert: 200 OK + body present + defaults reflected in returned metadata
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(i1, i2);
        assertThat(resp.getBody().totalCount()).isEqualTo(2L);
        assertThat(resp.getBody().page()).isEqualTo(1);
        assertThat(resp.getBody().pageSize()).isEqualTo(10);

        // Capture the Pageable passed to the service and verify sort/page
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        captor.capture());

        Pageable used = captor.getValue();
        // Controller translates 1-based page to 0-based for Spring Data
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(10);

        // Default sort must be by title ASC
        Sort.Order order = used.getSort().getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withFiltersAndPaging_areForwarded_andReflectedInResponse() {
        // Arrange
        String code = "ABC";
        String title = "Appeal";
        LocalDate sdFrom = LocalDate.of(2024, 1, 1);
        LocalDate sdTo = LocalDate.of(2024, 12, 31);
        LocalDate edFrom = LocalDate.of(2025, 1, 1);
        LocalDate edTo = LocalDate.of(2025, 12, 31);
        Integer pageParam = 2; // 1-based from client
        Integer sizeParam = 5;

        ResolutionCodeListItemDto item = org.mockito.Mockito.mock(ResolutionCodeListItemDto.class);
        Page<ResolutionCodeListItemDto> page = new PageImpl<>(List.of(item), PageRequest.of(1, 5), 42);

        when(service.search(
                        eq(code),
                        eq(title),
                        eq(sdFrom),
                        eq(sdTo),
                        eq(edFrom),
                        eq(edTo),
                        any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<ResolutionCodePageResponse> resp =
                controller.list(code, title, sdFrom, sdTo, edFrom, edTo, pageParam, sizeParam);

        // Assert response metadata & content
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().results()).containsExactly(item);
        assertThat(resp.getBody().totalCount()).isEqualTo(42L);
        assertThat(resp.getBody().page()).isEqualTo(2);
        assertThat(resp.getBody().pageSize()).isEqualTo(5);

        // Verify pageable details (converted to 0-based & sorted by title ASC)
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .search(
                        eq(code),
                        eq(title),
                        eq(sdFrom),
                        eq(sdTo),
                        eq(edFrom),
                        eq(edTo),
                        captor.capture());

        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        Sort.Order order = used.getSort().getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_whenInvalidPageOrSize_returns400_andDoesNotCallService() {
        // page < 1
        ResponseEntity<ResolutionCodePageResponse> r1 =
                controller.list(null, null, null, null, null, null, 0, 10);
        assertThat(r1.getStatusCodeValue()).isEqualTo(400);

        // size < 1
        ResponseEntity<ResolutionCodePageResponse> r2 =
                controller.list(null, null, null, null, null, null, 1, 0);
        assertThat(r2.getStatusCodeValue()).isEqualTo(400);

        // size > MAX_PAGE_SIZE (100)
        ResponseEntity<ResolutionCodePageResponse> r3 =
                controller.list(null, null, null, null, null, null, 1, 101);
        assertThat(r3.getStatusCodeValue()).isEqualTo(400);

        // No service calls should occur for invalid inputs
        verifyNoInteractions(service);
    }

    @Test
    void list_whenStartDateRangeInvalid_returns400_andNoServiceCall() {
        // startDateFrom after startDateTo should 400
        LocalDate from = LocalDate.of(2024, 12, 31);
        LocalDate to = LocalDate.of(2024, 1, 1);

        ResponseEntity<ResolutionCodePageResponse> r =
                controller.list(null, null, from, to, null, null, 1, 10);

        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void list_whenEndDateRangeInvalid_returns400_andNoServiceCall() {
        // endDateFrom after endDateTo should 400
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 1);

        ResponseEntity<ResolutionCodePageResponse> r =
                controller.list(null, null, null, null, from, to, 1, 10);

        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    @Test
    void getByCode_returnsOkWithBody() {
        // Arrange
        String code = "RC123";
        ResolutionCodeDto dto = org.mockito.Mockito.mock(ResolutionCodeDto.class);
        when(service.findByCode(code)).thenReturn(dto);

        // Act
        ResponseEntity<ResolutionCodeDto> resp = controller.getByCode(code);

        // Assert
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
        verify(service).findByCode(code);
    }

    @Test
    void getByCode_whenServiceThrows404_isPropagated() {
        // Arrange
        String code = "MISSING";
        when(service.findByCode(code))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        // Act + Assert: controller should propagate the exception (Spring maps it to 404)
        assertThrows(ResponseStatusException.class, () -> controller.getByCode(code));
        verify(service).findByCode(code);
    }
}
