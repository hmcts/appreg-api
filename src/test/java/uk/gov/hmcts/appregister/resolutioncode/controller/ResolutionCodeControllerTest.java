package uk.gov.hmcts.appregister.resolutioncode.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
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
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.service.ResolutionCodeService;
import uk.gov.hmcts.appregister.shared.validation.DateRangeValidator;

@ExtendWith(MockitoExtension.class)
class ResolutionCodeControllerTest {

    @Mock private ResolutionCodeService service;

    @Mock private DateRangeValidator dateRangeValidator;

    @InjectMocks private ResolutionCodeController controller;

    @Test
    void list_happyPath_callsValidatorAndService_returnsOkPage() {
        // Arrange: filters + pageable we pass to the controller
        String code = "rc";
        String title = "approved";
        LocalDate startFrom = LocalDate.of(2024, 1, 1);
        LocalDate startTo = LocalDate.of(2024, 12, 31);
        LocalDate endFrom = LocalDate.of(2025, 1, 1);
        LocalDate endTo = LocalDate.of(2025, 12, 31);
        Pageable pageable = PageRequest.of(1, 5);

        // Fake page returned by the service
        ResolutionCodeListItemDto item = new ResolutionCodeListItemDto(1L, "RC-001", "Approved");
        Page<ResolutionCodeListItemDto> page = new PageImpl<>(List.of(item), pageable, 7);

        when(service.search(
                        eq(code),
                        eq(title),
                        eq(startFrom),
                        eq(startTo),
                        eq(endFrom),
                        eq(endTo),
                        any(Pageable.class)))
                .thenReturn(page);

        // Act
        var response = controller.list(code, title, startFrom, startTo, endFrom, endTo, pageable);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(page);
        verify(dateRangeValidator).validate(startFrom, startTo, endFrom, endTo);

        // Verify pageable was forwarded unchanged
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .search(
                        eq(code),
                        eq(title),
                        eq(startFrom),
                        eq(startTo),
                        eq(endFrom),
                        eq(endTo),
                        pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
    }

    @Test
    void list_whenValidatorRejectsDateRange_throws400() {
        // Arrange: validator throws a BAD_REQUEST
        LocalDate startFrom = LocalDate.of(2024, 2, 1);
        LocalDate startTo = LocalDate.of(2024, 1, 1); // invalid range
        doThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.BAD_REQUEST, "bad range"))
                .when(dateRangeValidator)
                .validate(eq(startFrom), eq(startTo), isNull(), isNull());

        // Act + Assert: controller propagates the exception
        assertThrows(
                ResponseStatusException.class,
                () ->
                        controller.list(
                                null, null, startFrom, startTo, null, null, PageRequest.of(0, 10)));

        // Service should not be called
        verifyNoInteractions(service);
    }

    @Test
    void list_whenNoFilters_usesPageableAndReturnsPage() {
        // Arrange: no filters, just pageable
        Pageable pageable = PageRequest.of(0, 10);
        Page<ResolutionCodeListItemDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(service.search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        var response = controller.list(null, null, null, null, null, null, pageable);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(emptyPage);
        verify(dateRangeValidator).validate(null, null, null, null);
        verify(service)
                .search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getById_whenFound_returnsOkWithBody() {
        // Arrange
        var dto =
                new ResolutionCodeDto(
                        10L,
                        "RC-010",
                        "A Title",
                        "wording",
                        "legislation",
                        "dest1@ex.com",
                        "dest2@ex.com",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2025, 1, 1));
        when(service.findById(10L)).thenReturn(dto);

        // Act
        var response = controller.getById(10L);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(dto);
        verify(service).findById(10L);
    }

    @Test
    void getById_whenMissing_propagates404() {
        // Arrange
        when(service.findById(404L))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        // Act + Assert
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.getById(404L));
        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("not found");
        verify(service).findById(404L);
    }
}
