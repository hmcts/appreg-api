package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PageableMapperTest {

    @Test
    void testPageableTest() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        org.springframework.data.domain.Pageable pageable =
                appPageable.from(10, 2, List.of("field, 4334"), "defaultField", Sort.Direction.ASC);
        Assertions.assertEquals(10, pageable.getPageNumber());
        Assertions.assertEquals(2, pageable.getPageSize());
        Assertions.assertEquals("field", pageable.getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC, pageable.getSort().get().findFirst().get().getDirection());
    }

    @Test
    void testPageableDefaultSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        org.springframework.data.domain.Pageable pageable =
                appPageable.from(10, 2, null, "defaultField", Sort.Direction.ASC);
        Assertions.assertEquals(10, pageable.getPageNumber());
        Assertions.assertEquals(2, pageable.getPageSize());
        Assertions.assertEquals(
                "defaultField", pageable.getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC, pageable.getSort().get().findFirst().get().getDirection());
    }

    @Test
    void testPageableMultiSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        org.springframework.data.domain.Pageable pageable =
                appPageable.from(
                        10,
                        2,
                        List.of("field, DESC", "field2, ASC"),
                        "defaultField",
                        Sort.Direction.ASC);
        Assertions.assertEquals(10, pageable.getPageNumber());
        Assertions.assertEquals(2, pageable.getPageSize());
        Assertions.assertEquals("field", pageable.getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC, pageable.getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals("field2", pageable.getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC, pageable.getSort().get().toList().get(1).getDirection());
    }

    @Test
    void testPageableDefault() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(100);
        appPageable.setDefaultPageSize(23);

        org.springframework.data.domain.Pageable pageable =
                appPageable.from(
                        null,
                        null,
                        List.of("field,desc", "field2, ASC"),
                        "defaultField",
                        Sort.Direction.ASC);
        Assertions.assertEquals(0, pageable.getPageNumber());
        Assertions.assertEquals(23, pageable.getPageSize());
        Assertions.assertEquals("field", pageable.getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC, pageable.getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals("field2", pageable.getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC, pageable.getSort().get().toList().get(1).getDirection());
    }

    @Test
    void testPageableCapAtMaxSize() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(100);
        appPageable.setDefaultPageSize(23);

        org.springframework.data.domain.Pageable pageable =
                appPageable.from(
                        null,
                        300,
                        List.of("field, DESC", "field2, ASC"),
                        "defaultField",
                        Sort.Direction.ASC);
        Assertions.assertEquals(0, pageable.getPageNumber());
        Assertions.assertEquals(100, pageable.getPageSize());
        Assertions.assertEquals("field", pageable.getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC, pageable.getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals("field2", pageable.getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC, pageable.getSort().get().toList().get(1).getDirection());
    }
}
