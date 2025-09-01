package uk.gov.hmcts.appregister.resolutioncode.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.service.ResolutionCodeService;
import uk.gov.hmcts.appregister.shared.validation.DateRangeValidator;

@WebMvcTest(controllers = ResolutionCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = OAuth2ResourceServerAutoConfiguration.class)
class ResolutionCodeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ResolutionCodeService service;

    @MockitoBean DateRangeValidator dateRangeValidator;

    @Test
    void list_ok_withoutParams_usesDefaultSort_titleAsc_andReturnsPage() throws Exception {
        // Arrange
        Page<ResolutionCodeListItemDto> page =
                new PageImpl<>(
                        List.of(new ResolutionCodeListItemDto(1L, "RC-001", "Approved")),
                        PageRequest.of(0, 20, Sort.by(Sort.Order.asc("title"))),
                        1);
        given(
                        service.search(
                                isNull(),
                                isNull(),
                                isNull(),
                                isNull(),
                                isNull(),
                                isNull(),
                                any(Pageable.class)))
                .willReturn(page);

        // Act + Assert
        mockMvc.perform(get("/resolution-code"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].code").value("RC-001"))
                .andExpect(jsonPath("$.content[0].title").value("Approved"));

        // Verify default sort is title ASC
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service)
                .search(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        Sort.Order titleOrder = used.getSort().getOrderFor("title");
        assertThat(titleOrder).isNotNull();
        assertThat(titleOrder.getDirection()).isEqualTo(Sort.Direction.ASC);

        verifyNoMoreInteractions(service);
    }

    @Test
    void list_ok_withFilters_andClientPagingSorting() throws Exception {
        // Arrange
        String code = "rc";
        String title = "app";
        LocalDate startFrom = LocalDate.of(2024, 1, 1);
        LocalDate startTo = LocalDate.of(2024, 12, 31);
        LocalDate endFrom = LocalDate.of(2025, 1, 1);
        LocalDate endTo = LocalDate.of(2025, 12, 31);

        PageRequest clientPageable = PageRequest.of(1, 2, Sort.by(Sort.Order.desc("title")));
        Page<ResolutionCodeListItemDto> page =
                new PageImpl<>(
                        List.of(new ResolutionCodeListItemDto(2L, "RC-XYZ", "Some Title")),
                        clientPageable,
                        3);
        given(
                        service.search(
                                eq(code),
                                eq(title),
                                eq(startFrom),
                                eq(startTo),
                                eq(endFrom),
                                eq(endTo),
                                any(Pageable.class)))
                .willReturn(page);

        // Act + Assert
        mockMvc.perform(
                        get("/resolution-code")
                                .param("code", code)
                                .param("title", title)
                                .param("startDateFrom", "2024-01-01")
                                .param("startDateTo", "2024-12-31")
                                .param("endDateFrom", "2025-01-01")
                                .param("endDateTo", "2025-12-31")
                                .param("page", "1")
                                .param("size", "2")
                                .param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].code").value("RC-XYZ"))
                .andExpect(jsonPath("$.content[0].title").value("Some Title"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        verify(service)
                .search(
                        eq(code),
                        eq(title),
                        eq(startFrom),
                        eq(startTo),
                        eq(endFrom),
                        eq(endTo),
                        any(Pageable.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void getByCode_ok_returnsFullDto() throws Exception {
        // Arrange
        ResolutionCodeDto dto =
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
        given(service.findByCode("RC-010")).willReturn(dto);

        // Act + Assert (detail endpoint uses resultCode)
        mockMvc.perform(get("/resolution-code/{code}", "RC-010"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("A Title"))
                .andExpect(jsonPath("$.resultCode").value("RC-010"));
    }

    @Test
    void getByCode_404_whenServiceThrowsNotFound() throws Exception {
        given(service.findByCode("RC-404"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/resolution-code/{code}", "RC-404")).andExpect(status().isNotFound());
    }

    @Test
    void list_400_whenDateRangeValidatorRejectsInput() throws Exception {
        // Make validator cause a 400
        willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad range"))
                .given(dateRangeValidator)
                .validate(any(), any(), any(), any());

        mockMvc.perform(
                        get("/resolution-code")
                                .param("startDateFrom", "2025-12-31")
                                .param("startDateTo", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }
}
