package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListEntriesSummarySortFieldEnum;
import uk.gov.hmcts.appregister.common.api.TestSortableOperationEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;

class PageableMapperTest {

    @Test
    void testPageableTest() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        10,
                        2,
                        List.of(ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue() + ", asc"),
                        ApplicationEntrySortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(10, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(2, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue(),
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
    }

    @Test
    void testPageableDefaultSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        10,
                        2,
                        List.of(),
                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                        Sort.Direction.ASC,
                        ApplicationListEntriesSummarySortFieldEnum::getEntityValue);
        Assertions.assertEquals(10, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(2, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
    }

    @Test
    void testPageableMultiSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        10,
                        2,
                        List.of(
                                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue() + ", DESC",
                                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue()
                                        + ", ASC"),
                        ApplicationEntrySortFieldEnum.CJA_CODE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(10, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(2, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getEntityValue()[0],
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(2).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(2).getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue(),
                pageable.getSortStrings().get(0).getField());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                Sort.Direction.fromString(pageable.getSortStrings().get(0).getDirection()));
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                pageable.getSortStrings().get(1).getField());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                Sort.Direction.fromString(pageable.getSortStrings().get(1).getDirection()));
    }

    @Test
    void testPageableDefault() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(100);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        null,
                        List.of(
                                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue() + ", DESC",
                                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue()
                                        + ", ASC"),
                        ApplicationEntrySortFieldEnum.CJA_CODE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(0, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(23, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getEntityValue()[0],
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(2).getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(2).getProperty());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue(),
                pageable.getSortStrings().get(0).getField());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                Sort.Direction.fromString(pageable.getSortStrings().get(0).getDirection()));
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                pageable.getSortStrings().get(1).getField());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                Sort.Direction.fromString(pageable.getSortStrings().get(1).getDirection()));
    }

    @Test
    void testPageableCapAtMaxSize() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(100);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        300,
                        List.of(
                                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue() + ", DESC",
                                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue()
                                        + ", ASC"),
                        ApplicationEntrySortFieldEnum.CJA_CODE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);

        Assertions.assertEquals(100, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getEntityValue()[0],
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().toList().get(2).getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(2).getProperty());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.CJA_CODE.getApiValue(),
                pageable.getSortStrings().get(0).getField());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                Sort.Direction.fromString(pageable.getSortStrings().get(0).getDirection()));
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                pageable.getSortStrings().get(1).getField());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                Sort.Direction.fromString(pageable.getSortStrings().get(1).getDirection()));
    }

    @Test
    void testPageableSortDirectionFailure() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);
        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        10,
                                        2,
                                        List.of("field, 1232"),
                                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                                        Sort.Direction.ASC,
                                        ApplicationListEntriesSummarySortFieldEnum
                                                ::getEntityValue));
        Assertions.assertEquals(CommonAppError.SORT_DIRECTION_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableSortKeyFailure() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);
        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        10,
                                        2,
                                        List.of("field, asc"),
                                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                                        Sort.Direction.ASC,
                                        ApplicationListEntriesSummarySortFieldEnum
                                                ::getEntityValue));
        Assertions.assertEquals(CommonAppError.SORT_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableWithoutTieBreaker() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        300,
                        List.of(
                                TestSortableOperationEnum.TEST_NO_TIE_BREAKER.getApiValue()
                                        + ", DESC"),
                        TestSortableOperationEnum.TEST2_NO_TIE_BREAKER,
                        Sort.Direction.ASC,
                        TestSortableOperationEnum::getEntityValue);

        Assertions.assertEquals(1, pageable.getPageable().getSort().get().toList().size());
        Assertions.assertEquals(
                TestSortableOperationEnum.TEST_NO_TIE_BREAKER.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
    }

    @Test
    void testPageableWithoutTieBreakerDefaultSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        300,
                        List.of(),
                        TestSortableOperationEnum.TEST2_NO_TIE_BREAKER,
                        Sort.Direction.ASC,
                        TestSortableOperationEnum::getEntityValue);

        Assertions.assertEquals(1, pageable.getPageable().getSort().get().toList().size());
        Assertions.assertEquals(
                TestSortableOperationEnum.TEST2_NO_TIE_BREAKER.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
    }

    @Test
    void testPageableWithTieBreaker() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        300,
                        List.of(
                                TestSortableOperationEnum.TEST2_TIE_BREAKER.getApiValue()
                                        + ", DESC"),
                        TestSortableOperationEnum.TEST_TIE_BREAKER,
                        Sort.Direction.ASC,
                        TestSortableOperationEnum::getEntityValue);

        Assertions.assertEquals(
                TestSortableOperationEnum.TEST2_TIE_BREAKER.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                TestSortableOperationEnum.TEST2_TIE_BREAKER.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
    }
}
