package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;

public class PageMapperTest {

    @Test
    public void testBasicMappingAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "testSortField");
        Sort sort = Sort.by(order);
        pageable = pageable.withSort(sort);

        PageMapper mapper = new PageMapper();
        Page<?> page = new PageImpl<CriminalJusticeAreaGetDto>(List.of(), pageable, 25);

        PagingWrapper wrapper =
                PagingWrapper.of(SortableFieldMapper.of("apiSortField,desc"), pageable);
        CriminalJusticeAreaPage criminalJusticeAreaPage = new CriminalJusticeAreaPage();
        mapper.toPage(page, criminalJusticeAreaPage, wrapper.getSortStrings());

        Assertions.assertEquals(0, criminalJusticeAreaPage.getPageNumber());
        Assertions.assertEquals(10, criminalJusticeAreaPage.getPageSize());
        Assertions.assertEquals(3, criminalJusticeAreaPage.getTotalPages());
        Assertions.assertEquals(25, criminalJusticeAreaPage.getTotalElements());
        Assertions.assertEquals(
                SortOrdersInner.DirectionEnum.DESC,
                criminalJusticeAreaPage.getSort().getOrders().get(0).getDirection());
        Assertions.assertEquals(
                "apiSortField", criminalJusticeAreaPage.getSort().getOrders().get(0).getProperty());
    }

    @Test
    public void testBasicMappingNoSort() {
        PageRequest pageable = PageRequest.of(0, 10);

        PageMapper mapper = new PageMapper();
        Page<?> page = new PageImpl<CriminalJusticeAreaGetDto>(List.of(), pageable, 25);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), pageable);

        CriminalJusticeAreaPage criminalJusticeAreaPage = new CriminalJusticeAreaPage();
        mapper.toPage(page, criminalJusticeAreaPage, wrapper.getSortStrings());

        Assertions.assertEquals(0, criminalJusticeAreaPage.getPageNumber());
        Assertions.assertEquals(10, criminalJusticeAreaPage.getPageSize());
        Assertions.assertEquals(3, criminalJusticeAreaPage.getTotalPages());
        Assertions.assertEquals(25, criminalJusticeAreaPage.getTotalElements());
    }
}
