package uk.gov.hmcts.appregister.controller.applicationlist;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.filter.ApplicationListMixin;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.NameAddressMixin;
import uk.gov.hmcts.appregister.filter.applicationlist.ApplicationListFilterEnum;
import uk.gov.hmcts.appregister.filter.applicationlist.ApplicationListSortEnum;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;
import uk.gov.hmcts.appregister.util.CopyUtil;

public class ApplicationListFilterAndSortTest
        extends AbstractFilterAndSortControllerTest<ApplicationList> {

    @Override
    protected Stream<RestFilterEndpointDescription<ApplicationList>> getFilterDescriptions()
            throws Exception {
        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);

        // create the application list
        ApplicationList applicationCode = new AppListTestData().someComplete();
        applicationCode.setEntries(List.of());

        // process the scenario
        FilterableScenario<ApplicationList> scenario =
                FilterScenarioFactory.createFilterScenario(
                        applicationCode,
                        Arrays.asList(ApplicationListFilterEnum.values()),
                        Arrays.asList(ApplicationListSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<ApplicationList> restFilterDescription =
                new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("application-lists"));
        restFilterDescription.setSortDescriptors(Arrays.asList(ApplicationListSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(
                restFilterDescription
                        .getForScenario(filterScenarioStrategy)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ApplicationList>> getSortDescriptions()
            throws Exception {
        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);

        // create the application code
        ApplicationList applicationCode = new AppListTestData().someComplete();
        applicationCode.setEntries(List.of());

        // process the scenario
        List<ApplicationList> applicationLists =
                FilterScenarioFactory.createSort(
                        applicationCode, Arrays.asList(ApplicationListSortEnum.values()));

        List<RestSortEndpointDescription<ApplicationList>> sortEndpointDescriptions =
                new ArrayList<>();
        for (ApplicationListSortEnum applicationCodeSortEnum : ApplicationListSortEnum.values()) {
            RestSortEndpointDescription<ApplicationList> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("application-lists"));
            restFilterDescription.setSortDescriptors(applicationCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(applicationLists);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(ApplicationListSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(
            List<ApplicationList> keyable, Response response, List<ApplicationList> exclude) {
        ApplicationListPage page = response.as(ApplicationListPage.class);
        List<ApplicationListGetSummaryDto> content = page.getContent();

        // assert the excludes are not in the response
        assertExcluded(response, exclude);

        // assert the order of the response is correct and all keys that are expected are there
        int expectedIndex = 0;

        for (ApplicationListGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getUuid().equals(item.getId())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    private void assertExcluded(Response response, List<ApplicationList> exclude) {
        ApplicationListPage page = response.as(ApplicationListPage.class);
        List<ApplicationListGetSummaryDto> content = page.getContent();
        for (ApplicationList keyable : exclude) {
            Assertions.assertFalse(
                    content.stream().anyMatch(dto -> dto.getId().equals(keyable.getUuid())));
        }
    }

    @Override
    protected ApplicationList saveToDatabase(ApplicationList keyable) {
        return this.persistance.save(keyable);
    }

    private void assertKeyableForSummary(
            ApplicationList keyable, ApplicationListGetSummaryDto dto) {
        Assertions.assertEquals(keyable.getDate(), dto.getDate());
        Assertions.assertEquals(keyable.getTime(), dto.getTime());
        Assertions.assertEquals(keyable.getStatus().getValue(), dto.getStatus().getValue());
        Assertions.assertEquals(keyable.getEntries().size(), dto.getEntriesCount());
        Assertions.assertEquals(keyable.getDescription(), dto.getDescription());
        Assertions.assertEquals(
                keyable.getCourtName() == null ? keyable.getDescription() : keyable.getCourtName(),
                dto.getLocation());
    }
}
