package uk.gov.hmcts.appregister.nationalcourthouse.controller;

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
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.service.NationalCourtHouseService;
import uk.gov.hmcts.appregister.shared.validation.DateRangeValidator;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseControllerTest {

    @Mock private NationalCourtHouseService service;

    @Mock private DateRangeValidator dateRangeValidator;

    @InjectMocks private NationalCourtHouseController controller;

    @Test
    void list_happyPath_callsValidatorAndService_returnsOkPage() {
        // Arrange: filters + pageable we pass into the controller
        String name = "card";
        String courtType = "CROWN";
        LocalDate startFrom = LocalDate.of(2020, 1, 1);
        LocalDate startTo = LocalDate.of(2020, 12, 31);
        LocalDate endFrom = LocalDate.of(2021, 1, 1);
        LocalDate endTo = LocalDate.of(2022, 1, 1);
        Pageable pageable = PageRequest.of(1, 5); // zero-based page index

        // Fake page returned by the service
        NationalCourtHouseDto dto1 =
                new NationalCourtHouseDto(
                        1L,
                        "Cardiff",
                        "CROWN",
                        LocalDate.of(2019, 1, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        Page<NationalCourtHouseDto> page = new PageImpl<>(List.of(dto1), pageable, 7);

        when(service.search(
                        eq(name),
                        eq(courtType),
                        eq(startFrom),
                        eq(startTo),
                        eq(endFrom),
                        eq(endTo),
                        any(Pageable.class)))
                .thenReturn(page);

        // Act
        var response =
                controller.list(name, courtType, startFrom, startTo, endFrom, endTo, pageable);

        // Assert: 200 OK with body, validator called once with the ranges
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(page);
        verify(dateRangeValidator).validate(startFrom, startTo, endFrom, endTo);

        // Also assert that the pageable we passed is forwarded to the service
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .search(
                        eq(name),
                        eq(courtType),
                        eq(startFrom),
                        eq(startTo),
                        eq(endFrom),
                        eq(endTo),
                        pageableCaptor.capture());
        Pageable usedPageable = pageableCaptor.getValue();
        assertThat(usedPageable.getPageNumber()).isEqualTo(1);
        assertThat(usedPageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void list_whenValidatorRejectsDateRange_throws400() {
        // Arrange: have the validator throw a 400 ResponseStatusException
        LocalDate startFrom = LocalDate.of(2024, 2, 1);
        LocalDate startTo = LocalDate.of(2024, 1, 1); // invalid range: from > to
        doThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.BAD_REQUEST, "bad range"))
                .when(dateRangeValidator)
                .validate(eq(startFrom), eq(startTo), isNull(), isNull());

        // Act + Assert: calling list should propagate the exception (controller does not catch it)
        assertThrows(
                ResponseStatusException.class,
                () ->
                        controller.list(
                                null, null, startFrom, startTo, null, null, PageRequest.of(0, 10)));

        // Service must not be called when validation fails
        verifyNoInteractions(service);
    }

    @Test
    void list_whenNoFilters_usesPageableAndReturnsPage() {
        // Arrange: no filters, just a pageable
        Pageable pageable = PageRequest.of(0, 10);
        Page<NationalCourtHouseDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
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

        // Assert: OK + empty page and proper delegation
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(emptyPage);
        verify(dateRangeValidator).validate(null, null, null, null);
        verify(service)
                .search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getById_whenFound_returnsOkWithBody() {
        // Arrange
        Long id = 99L;
        NationalCourtHouseDto dto =
                new NationalCourtHouseDto(
                        id,
                        "Leeds",
                        "CROWN",
                        LocalDate.of(2018, 1, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        when(service.findById(id)).thenReturn(dto);

        // Act
        var response = controller.getById(id);

        // Assert: simply returns 200 with the DTO, service invoked once
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(dto);
        verify(service).findById(id);
    }

    @Test
    void getById_whenMissing_propagates404() {
        // Arrange: service throws a 404 ResponseStatusException
        Long id = 404L;
        when(service.findById(id))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        // Act + Assert: controller propagates the exception; it will be translated by Spring MVC
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.getById(id));
        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("not found");
        verify(service).findById(id);
    }
}
