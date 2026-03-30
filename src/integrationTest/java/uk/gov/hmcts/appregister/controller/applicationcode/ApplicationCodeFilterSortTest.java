package uk.gov.hmcts.appregister.controller.applicationcode;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.applicationcode.ApplicationCodeFilterEnum;
import uk.gov.hmcts.appregister.data.filter.applicationcode.ApplicationCodeSortEnum;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

public class ApplicationCodeFilterSortTest
        extends AbstractFilterAndSortControllerTest<ApplicationCode> {

    @Override
    protected Stream<RestFilterEndpointDescription<ApplicationCode>> getFilterDescriptions()
            throws Exception {
        // create the application code
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        // process the scenario
        FilterableScenario<ApplicationCode> scenario =
                FilterScenarioFactory.createFilterScenario(
                        applicationCode,
                        Arrays.asList(ApplicationCodeFilterEnum.values()),
                        Arrays.asList(ApplicationCodeSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<ApplicationCode> restFilterDescription =
                new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("application-codes"));
        restFilterDescription.setSortDescriptors(Arrays.asList(ApplicationCodeSortEnum.values()));

        // gets all of the combinations of filters
        return Stream.of(
                restFilterDescription
                        .getForScenario(scenario)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ApplicationCode>> getSortDescriptions()
            throws Exception {
        // create the application code
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        // process the scenario
        List<ApplicationCode> applicationCodes =
                FilterScenarioFactory.createSort(
                        applicationCode, Arrays.asList(ApplicationCodeSortEnum.values()));

        // get all of the sort endpoint descriptions
        List<RestSortEndpointDescription<ApplicationCode>> sortEndpointDescriptions =
                new ArrayList<>();
        for (ApplicationCodeSortEnum applicationCodeSortEnum : ApplicationCodeSortEnum.values()) {
            RestSortEndpointDescription<ApplicationCode> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("application-codes"));
            restFilterDescription.setSortDescriptors(applicationCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(applicationCodes);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(ApplicationCodeSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        ApplicationCodePage page = response.as(ApplicationCodePage.class);
        return size == page.getContent().size();
    }

    @Override
    protected ApplicationCode saveToDatabase(ApplicationCode keyable) {
        return this.persistance.save(keyable);
    }

    @Override
    protected boolean assertResponseInOrder(List<ApplicationCode> keyable, Response response) {
        ApplicationCodePage page = response.as(ApplicationCodePage.class);
        List<ApplicationCodeGetSummaryDto> content = page.getContent();

        int expectedIndex = 0;

        for (ApplicationCodeGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getCode().equals(item.getApplicationCode())) {
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }
}
