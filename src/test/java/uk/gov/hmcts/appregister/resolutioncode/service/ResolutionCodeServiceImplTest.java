package uk.gov.hmcts.appregister.resolutioncode.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;

@ExtendWith(MockitoExtension.class)
class ResolutionCodeServiceImplTest {

    @Mock private ResolutionCodeRepository repository;
    @Mock private ResolutionCodeMapper mapper;

    private ResolutionCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResolutionCodeServiceImpl(repository, mapper);
    }

    // ---------- findAll ----------

    @Test
    void findAll_sortsByNameAsc_andMapsAll() {
        ResolutionCode e1 = ResolutionCode.builder().id(1L).resultCode("RC-1").title("A").build();
        ResolutionCode e2 = ResolutionCode.builder().id(2L).resultCode("RC-2").title("B").build();

        when(repository.findAll(any(Sort.class))).thenReturn(List.of(e1, e2));
        when(mapper.toReadDto(e1)).thenReturn(Optional.of(dto(1L, "RC-1", "A")));
        when(mapper.toReadDto(e2)).thenReturn(Optional.of(dto(2L, "RC-2", "B")));

        List<ResolutionCodeDto> out = service.findAll();

        assertThat(out).extracting(ResolutionCodeDto::id).containsExactly(1L, 2L);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        Sort sort = sortCaptor.getValue();
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAll_filtersOutEmptyOptionals_fromMapper() {
        ResolutionCode e1 = ResolutionCode.builder().id(1L).resultCode("RC-1").title("A").build();
        ResolutionCode e2 = ResolutionCode.builder().id(2L).resultCode("RC-2").title("B").build();

        when(repository.findAll(any(Sort.class))).thenReturn(List.of(e1, e2));
        when(mapper.toReadDto(e1)).thenReturn(Optional.empty()); // simulate a mapping gap
        when(mapper.toReadDto(e2)).thenReturn(Optional.of(dto(2L, "RC-2", "B")));

        List<ResolutionCodeDto> out = service.findAll();

        assertThat(out).singleElement().extracting(ResolutionCodeDto::id).isEqualTo(2L);
    }

    // ---------- findByCode ----------

    @Test
    void findById_404_whenRepositoryEmpty() {
        when(repository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(123L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findById_ok_whenMapperReturnsDto() {
        ResolutionCode entity =
                ResolutionCode.builder().id(9L).resultCode("RC-9").title("Nine").build();
        ResolutionCodeDto dto = dto(9L, "RC-9", "Nine");

        when(repository.findById(9L)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(Optional.of(dto));

        ResolutionCodeDto out = service.findById(9L);

        assertThat(out).isEqualTo(dto);
        verify(repository).findById(9L);
        verify(mapper).toReadDto(entity);
    }

    @Test
    void findById_404_whenMapperReturnsEmptyForEntity() {
        ResolutionCode entity =
                ResolutionCode.builder().id(7L).resultCode("RC-7").title("Seven").build();

        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(mapper.toReadDto(entity)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(7L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- search ----------

    @Test
    void search_passesThroughFiltersAndPageable_andMapsContent() {
        // Inputs:
        String code = "rc";
        String title = "app";
        LocalDate startDateFrom = LocalDate.of(2024, 1, 1);
        LocalDate startDateTo = LocalDate.of(2024, 12, 31);
        LocalDate endDateFrom = LocalDate.of(2025, 1, 1);
        LocalDate endDateTo = LocalDate.of(2025, 12, 31);
        Pageable pageable = PageRequest.of(1, 3, Sort.by("title").descending());

        // Repository returns entities:
        ResolutionCode e1 = ResolutionCode.builder().id(1L).resultCode("RC-1").title("A").build();
        ResolutionCode e2 = ResolutionCode.builder().id(2L).resultCode("RC-2").title("B").build();
        Page<ResolutionCode> repoPage = new PageImpl<>(List.of(e1, e2), pageable, 5);

        when(repository.search(
                        eq(code),
                        eq(title),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        eq(pageable)))
                .thenReturn(repoPage);

        // Mapper for list items:
        ResolutionCodeListItemDto i1 = new ResolutionCodeListItemDto(1L, "RC-1", "A");
        ResolutionCodeListItemDto i2 = new ResolutionCodeListItemDto(2L, "RC-2", "B");
        when(mapper.toListItem(e1)).thenReturn(Optional.of(i1));
        when(mapper.toListItem(e2)).thenReturn(Optional.of(i2));

        Page<ResolutionCodeListItemDto> out =
                service.search(
                        code, title, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable);

        assertThat(out.getContent()).containsExactly(i1, i2);
        assertThat(out.getNumber()).isEqualTo(1);
        assertThat(out.getSize()).isEqualTo(3);
        assertThat(out.getTotalElements()).isEqualTo(5);

        verify(repository)
                .search(
                        eq(code),
                        eq(title),
                        eq(startDateFrom),
                        eq(startDateTo),
                        eq(endDateFrom),
                        eq(endDateTo),
                        eq(pageable));
        verify(mapper).toListItem(e1);
        verify(mapper).toListItem(e2);
    }

    @Test
    void search_throwsIllegalState_whenMapperReturnsEmptyForAnyEntity() {
        Pageable pageable = PageRequest.of(0, 2);
        ResolutionCode e1 = ResolutionCode.builder().id(1L).resultCode("RC-1").title("A").build();

        when(repository.search(any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(e1), pageable, 1));
        when(mapper.toListItem(e1)).thenReturn(Optional.empty()); // should explode

        assertThatThrownBy(() -> service.search(null, null, null, null, null, null, pageable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mapper returned empty Optional for non-null entity");
    }

    // ---------- helpers ----------

    private static ResolutionCodeDto dto(Long id, String code, String title) {
        return new ResolutionCodeDto(
                id,
                code,
                title,
                "wording",
                "legislation",
                "dest1@example.com",
                "dest2@example.com",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1));
    }
}
