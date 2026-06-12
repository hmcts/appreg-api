package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntryDefaultSortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortConfig;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListEntriesSummarySortFieldEnum;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
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
                        List.of(
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
                                        + ", asc"),
                        ApplicationEntrySortFieldEnum.APPLICANT,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(10, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(2, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue(),
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.ASC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getTieBreaker(),
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
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
                                        + ", DESC"),
                        ApplicationEntrySortFieldEnum.APPLICATION_TITLE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(10, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(2, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
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
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
                                        + ", DESC"),
                        ApplicationEntrySortFieldEnum.APPLICATION_TITLE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);
        Assertions.assertEquals(0, pageable.getPageable().getPageNumber());
        Assertions.assertEquals(23, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
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
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
                                        + ", DESC"),
                        ApplicationEntrySortFieldEnum.APPLICATION_TITLE,
                        Sort.Direction.ASC,
                        ApplicationEntrySortFieldEnum::getEntityValue);

        Assertions.assertEquals(100, pageable.getPageable().getPageSize());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
    }

    @Test
    void testPageableSortDirectionFailure() {
        var appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);
        var sort = List.of("field, 1232");
        var ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        10,
                                        2,
                                        sort,
                                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                                        Sort.Direction.ASC,
                                        ApplicationListEntriesSummarySortFieldEnum
                                                ::getEntityValue));
        Assertions.assertEquals(CommonAppError.SORT_DIRECTION_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableSortKeyFailure() {
        var appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);
        var sort = List.of("field, asc");
        var ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        10,
                                        2,
                                        sort,
                                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                                        Sort.Direction.ASC,
                                        ApplicationListEntriesSummarySortFieldEnum
                                                ::getEntityValue));
        Assertions.assertEquals(CommonAppError.SORT_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableSupportsInternalDefaultSortOutsideExternalSortLookup() {
        var appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        var pageable =
                appPageable.from(
                        null,
                        null,
                        List.of(),
                        new SortConfig(
                                InternalDefaultSortField.INTERNAL_ONLY,
                                TestSortableOperationEnum::getEntityValue),
                        Sort.Direction.ASC);

        Assertions.assertEquals(
                InternalDefaultSortField.INTERNAL_ONLY.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                InternalDefaultSortField.INTERNAL_ONLY.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());

        var sort = List.of(InternalDefaultSortField.INTERNAL_ONLY.getApiValue() + ",asc");
        var ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        null,
                                        null,
                                        sort,
                                        InternalDefaultSortField.INTERNAL_ONLY,
                                        Sort.Direction.ASC,
                                        TestSortableOperationEnum::getEntityValue));
        Assertions.assertEquals(CommonAppError.SORT_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableUsingSortConfig() {
        var appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        var pageable =
                appPageable.from(
                        null,
                        null,
                        List.of(),
                        ApplicationEntrySortConfig.SEARCH,
                        Sort.Direction.ASC);

        Assertions.assertEquals(
                ApplicationEntryDefaultSortFieldEnum.CODE.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                ApplicationEntryDefaultSortFieldEnum.CODE.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());

        var sort = List.of(ApplicationEntryDefaultSortFieldEnum.CODE.getApiValue() + ",asc");
        var ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                appPageable.from(
                                        null,
                                        null,
                                        sort,
                                        ApplicationEntrySortConfig.SEARCH,
                                        Sort.Direction.ASC));
        Assertions.assertEquals(CommonAppError.SORT_NOT_SUITABLE, ex.getCode());
    }

    @Test
    void testPageableUsingApplicationEntryIsResultedSort() {
        PageableMapper appPageable = new PageableMapper();
        appPageable.setMaxPageSize(10);
        appPageable.setDefaultPageSize(23);

        PagingWrapper pageable =
                appPageable.from(
                        null,
                        null,
                        List.of(ApplicationEntrySortFieldEnum.IS_RESULTED.getApiValue() + ",desc"),
                        ApplicationEntrySortConfig.SEARCH,
                        Sort.Direction.ASC);

        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.IS_RESULTED.getEntityValue()[0],
                pageable.getPageable().getSort().get().findFirst().get().getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().findFirst().get().getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.IS_RESULTED.getTieBreaker(),
                pageable.getPageable().getSort().get().toList().get(1).getProperty());
        Assertions.assertEquals(
                Sort.Direction.DESC,
                pageable.getPageable().getSort().get().toList().get(1).getDirection());
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.IS_RESULTED.getApiValue(),
                pageable.getSortStrings().getFirst().getField());
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

    private enum InternalDefaultSortField implements SortableOperationEnum {
        INTERNAL_ONLY("internalOnly", "id", "internalOnly");

        private final String apiValue;
        private final String tieBreaker;
        private final String[] entityValue;

        InternalDefaultSortField(String apiValue, String tieBreaker, String... entityValue) {
            this.apiValue = apiValue;
            this.tieBreaker = tieBreaker;
            this.entityValue = entityValue;
        }

        @Override
        public String getApiValue() {
            return apiValue;
        }

        @Override
        public String[] getEntityValue() {
            return entityValue;
        }

        @Override
        public String getTieBreaker() {
            return tieBreaker;
        }
    }
}
