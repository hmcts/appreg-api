package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.config.ApplicationListSortProperties;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;

@ExtendWith(MockitoExtension.class)
class ApplicationListControllerTest {

    @Mock private ApplicationListService service;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter counter;

    private ApplicationListController controller;

    @BeforeEach
    void setUp() {
        PageableMapper pageableMapper = new PageableMapper();
        pageableMapper.setMaxPageSize(100);
        pageableMapper.setDefaultPageSize(10);

        ApplicationListSortProperties sortProperties = new ApplicationListSortProperties();
        sortProperties.setDisabledSortKeys(
                List.of(ApplicationListSortFieldEnum.TIME.getApiValue()));

        controller =
                new ApplicationListController(
                        service, pageableMapper, sortProperties, meterRegistry);
    }

    @Test
    void givenMultipleSortsWithDisabledFirstSortKey_whenGetApplicationLists_thenThrowsBadRequest() {
        // TIME is configured as a disabled sort key for application lists.
        when(meterRegistry.counter(
                        "appreg.sort.ignored",
                        "sortKey",
                        ApplicationListSortFieldEnum.TIME.getApiValue()))
                .thenReturn(counter);

        // Expected behaviour:
        // even if the first requested sort key is disabled, a request with multiple sort values
        // should still be rejected with MULTIPLE_SORT_NOT_SUPPORTED rather than silently
        // falling back to the default description ordering.
        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                controller.getApplicationLists(
                                        new ApplicationListGetFilterDto(),
                                        0,
                                        10,
                                        List.of(
                                                ApplicationListSortFieldEnum.TIME.getApiValue()
                                                        + ",desc",
                                                ApplicationListSortFieldEnum.STATUS.getApiValue()
                                                        + ",asc")));

        assertEquals(CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED, ex.getCode());
        verifyNoInteractions(service);
    }
}
