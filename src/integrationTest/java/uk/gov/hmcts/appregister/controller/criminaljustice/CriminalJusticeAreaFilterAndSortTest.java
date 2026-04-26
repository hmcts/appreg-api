package uk.gov.hmcts.appregister.controller.criminaljustice;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.criminaljusticearea.CriminalJusticeAreaFilterEnum;
import uk.gov.hmcts.appregister.filter.criminaljusticearea.CriminalJusticeAreaSortEnum;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

public class CriminalJusticeAreaFilterAndSortTest
        extends AbstractFilterAndSortControllerTest<CriminalJusticeArea> {

    @Override
    protected Stream<RestFilterEndpointDescription<CriminalJusticeArea>> getFilterDescriptions()
            throws Exception {
        // create the application code
        CriminalJusticeArea criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        // process the scenario
        FilterableScenario<CriminalJusticeArea> scenario =
                FilterScenarioFactory.createFilterScenario(
                        criminalJusticeArea,
                        Arrays.asList(CriminalJusticeAreaFilterEnum.values()),
                        Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<CriminalJusticeArea> restFilterDescription =
                new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("criminal-justice-areas"));
        restFilterDescription.setSortDescriptors(
                Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(
                restFilterDescription
                        .getForScenario(filterScenarioStrategy)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<CriminalJusticeArea>> getSortDescriptions()
            throws Exception {
        // create the application code
        CriminalJusticeArea nationalCourtHouse = new CriminalJusticeTestData().someComplete();

        // process the scenario
        List<CriminalJusticeArea> criminalJusticeAreas =
                FilterScenarioFactory.createSort(
                        nationalCourtHouse, Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        List<RestSortEndpointDescription<CriminalJusticeArea>> sortEndpointDescriptions =
                new ArrayList<>();
        for (CriminalJusticeAreaSortEnum criminalJusticeAreaSortEnum :
                CriminalJusticeAreaSortEnum.values()) {
            RestSortEndpointDescription<CriminalJusticeArea> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("criminal-justice-areas"));
            restFilterDescription.setSortDescriptors(criminalJusticeAreaSortEnum);
            restFilterDescription.setExpectedToBeGenerated(criminalJusticeAreas);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(CriminalJusticeAreaSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(
            List<CriminalJusticeArea> keyable,
            Response response,
            List<CriminalJusticeArea> exclude) {
        CriminalJusticeAreaPage page = response.as(CriminalJusticeAreaPage.class);
        List<CriminalJusticeAreaGetDto> content = page.getContent();

        // assert the excludes are not in the response
        assertExcluded(response, exclude);

        // assert the order of the response is correct and all keys that are expected are there
        int expectedIndex = 0;

        for (CriminalJusticeAreaGetDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getCode().equals(item.getCode())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    private void assertExcluded(Response response, List<CriminalJusticeArea> exclude) {
        CriminalJusticeAreaPage page = response.as(CriminalJusticeAreaPage.class);
        List<CriminalJusticeAreaGetDto> content = page.getContent();

        for (CriminalJusticeAreaGetDto item : content) {
            for (CriminalJusticeArea excluded : exclude) {
                if (excluded.getCode().equals(item.getCode())) {
                    throw new FilterProcessingException(
                            "Excluded code %s was found in the response"
                                    .formatted(excluded.getCode()));
                }
            }
        }
    }

    private void assertKeyableForSummary(
            CriminalJusticeArea keyable, CriminalJusticeAreaGetDto dto) {
        Assertions.assertEquals(keyable.getCode(), dto.getCode());
        Assertions.assertEquals(keyable.getDescription(), dto.getDescription());
    }

    @Override
    protected CriminalJusticeArea saveToDatabase(CriminalJusticeArea keyable) {
        return this.persistance.save(keyable);
    }
}
