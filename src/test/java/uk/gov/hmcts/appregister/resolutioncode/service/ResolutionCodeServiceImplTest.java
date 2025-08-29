package uk.gov.hmcts.appregister.resolutioncode.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;
import uk.gov.hmcts.appregister.resolutioncode.repository.ResolutionCodeRepository;

@ExtendWith(MockitoExtension.class)
class ResolutionCodeServiceImplTest {

    @Mock private ResolutionCodeRepository repository;
    @Mock private ResolutionCodeMapper mapper;

    @InjectMocks private ResolutionCodeServiceImpl service;

    // Helper: build a Page with a specific totalElements so assertions match exactly
    private static Page<ResolutionCode> pageWithTotal(
        List<ResolutionCode> content, Pageable pageable, long total) {
        return new PageImpl<>(content, pageable, total);
    }

    // ---------------- findAll ----------------

    @Test
    void findAll_mapsEntitiesToDtos_andReturnsList() {
        ResolutionCode e1 = ResolutionCode.builder().id(1L).build();
        ResolutionCode e2 = ResolutionCode.builder().id(2L).build();
        when(repository.findAll()).thenReturn(List.of(e1, e2));

        ResolutionCodeDto d1 =
                new ResolutionCodeDto(
                        1L,
                        "RC1",
                        "Title1",
                        "Wording1",
                        "Leg1",
                        "a@b.com",
                        null,
                        LocalDate.parse("2020-01-01"),
                        null);
        ResolutionCodeDto d2 =
                new ResolutionCodeDto(
                        2L,
                        "RC2",
                        "Title2",
                        "Wording2",
                        null,
                        null,
                        "c@d.com",
                        LocalDate.parse("2020-02-02"),
                        LocalDate.parse("2020-12-31"));

        when(mapper.toReadDto(e1)).thenReturn(d1);
        when(mapper.toReadDto(e2)).thenReturn(d2);

        List<ResolutionCodeDto> out = service.findAll();

        assertThat(out).containsExactly(d1, d2);
        verify(repository).findAll();
        verify(mapper).toReadDto(e1);
        verify(mapper).toReadDto(e2);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void findAll_whenNone_returnsEmpty_andDoesNotMap() {
        when(repository.findAll()).thenReturn(List.of());

        List<ResolutionCodeDto> out = service.findAll();

        assertThat(out).isEmpty();
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    // ---------------- findByCode ----------------

    @Test
    void findByCode_whenFound_mapsAndReturnsDto() {
        String code = "RC99";
        ResolutionCode entity = ResolutionCode.builder().id(99L).resultCode(code).title("X").build();
        ResolutionCodeDto dto =
                new ResolutionCodeDto(
                        99L, code, "X", "W", null, null, null, LocalDate.parse("2022-01-01"), null);

        when(repository.findByResultCode(code)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(dto);

        ResolutionCodeDto out = service.findByCode(code);

        assertThat(out).isEqualTo(dto);
        verify(repository).findByResultCode(code);
        verify(mapper).toReadDto(entity);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void findByCode_whenMissing_throws404_andDoesNotMap() {
        when(repository.findByResultCode(anyString())).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.findByCode("NOPE"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository).findByResultCode("NOPE");
        verifyNoInteractions(mapper);
    }

    // ---------------- search ----------------

    @Test
    void search_withCodeOnly_buildsSpec_andMapsListItems() {
        String code = "RC1";
        Pageable pageable = PageRequest.of(0, 5);

        ResolutionCode e = ResolutionCode.builder().id(1L).resultCode("RC123").title("Alpha").build();
        ResolutionCodeListItemDto dto = new ResolutionCodeListItemDto(1L, "RC123", "Alpha");

        Page<ResolutionCode> repoPage = pageWithTotal(List.of(e), pageable, 1);
        when(repository.findAll(
            ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toListItem(e)).thenReturn(dto);

        Page<ResolutionCodeListItemDto> out =
                service.search(code, null, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<ResolutionCode>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withTitleOnly_buildsSpec_andMapsListItems() {
        String title = "Dismiss";
        Pageable pageable = PageRequest.of(0, 10);

        ResolutionCode e = ResolutionCode.builder().id(2L).resultCode("RC2").title("Dismissed").build();
        ResolutionCodeListItemDto dto = new ResolutionCodeListItemDto(2L, "RC2", "Dismissed");

        // Set totalElements to exactly 4 to satisfy the assertion
        Page<ResolutionCode> repoPage = pageWithTotal(List.of(e), pageable, 4);
        when(repository.findAll(
            ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toListItem(e)).thenReturn(dto);

        Page<ResolutionCodeListItemDto> out =
                service.search(null, title, null, null, null, null, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<ResolutionCode>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withAllFilters_buildsSpec_andMapsListItems() {
        String code = "RC";
        String title = "Appeal";

        LocalDate startFrom = LocalDate.parse("2020-01-01");
        LocalDate startTo = LocalDate.parse("2021-12-31");
        LocalDate endFrom = LocalDate.parse("2022-01-01");
        LocalDate endTo = LocalDate.parse("2024-01-01");

        Pageable pageable = PageRequest.of(1, 5);

        ResolutionCode e =
                ResolutionCode.builder().id(11L).resultCode("RC11").title("Appeal Granted").build();
        ResolutionCodeListItemDto dto = new ResolutionCodeListItemDto(11L, "RC11", "Appeal Granted");

        // Set totalElements to exactly 9 (your expected value)
        Page<ResolutionCode> repoPage = pageWithTotal(List.of(e), pageable, 9);
        when(repository.findAll(
            ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toListItem(e)).thenReturn(dto);

        Page<ResolutionCodeListItemDto> out =
                service.search(code, title, startFrom, startTo, endFrom, endTo, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(6);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<ResolutionCode>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withDateBounds_buildsCompositeSpec_andMaps() {
        LocalDate startFrom = LocalDate.parse("2020-01-01");
        LocalDate startTo = LocalDate.parse("2020-12-31");
        LocalDate endFrom = LocalDate.parse("2021-01-01");
        LocalDate endTo = LocalDate.parse("2023-01-01");

        Pageable pageable = PageRequest.of(0, 10);

        ResolutionCode e = ResolutionCode.builder().id(3L).resultCode("RC3").title("T").build();
        ResolutionCodeListItemDto dto = new ResolutionCodeListItemDto(3L, "RC3", "T");

        // Set totalElements to exactly 3 (your expected value)
        Page<ResolutionCode> repoPage = pageWithTotal(List.of(e), pageable, 3);
        when(repository.findAll(
            ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toListItem(e)).thenReturn(dto);

        Page<ResolutionCodeListItemDto> out =
                service.search(null, null, startFrom, startTo, endFrom, endTo, pageable);

        assertThat(out.getContent()).containsExactly(dto);
        assertThat(out.getTotalElements()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<ResolutionCode>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void search_withNoFilters_returnsEmptyPage_andSkipsMapper() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ResolutionCode> repoPage = pageWithTotal(List.of(), pageable, 0);

        when(repository.findAll(
            ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class)))
                .thenReturn(repoPage);

        Page<ResolutionCodeListItemDto> out =
                service.search(null, null, null, null, null, null, pageable);

        assertThat(out.getContent()).isEmpty();
        assertThat(out.getTotalElements()).isEqualTo(0);
        verify(repository)
                .findAll(ArgumentMatchers.<Specification<ResolutionCode>>any(), any(Pageable.class));
        verifyNoInteractions(mapper);
    }
}
