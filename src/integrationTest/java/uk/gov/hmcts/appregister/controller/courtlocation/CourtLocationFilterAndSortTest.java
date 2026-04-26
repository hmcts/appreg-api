package uk.gov.hmcts.appregister.controller.courtlocation;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.courtlocation.CourtLocationFilterEnum;
import uk.gov.hmcts.appregister.filter.courtlocation.CourtLocationSortEnum;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

@Slf4j
public class CourtLocationFilterAndSortTest
        extends AbstractFilterAndSortControllerTest<NationalCourtHouse> {

    @Override
    protected Stream<RestFilterEndpointDescription<NationalCourtHouse>> getFilterDescriptions()
            throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);
        nationalCourtHouse.setStartDate(LocalDate.now().minusDays(1));

        // process the scenario
        FilterableScenario<NationalCourtHouse> scenario =
                FilterScenarioFactory.createFilterScenario(
                        nationalCourtHouse,
                        Arrays.asList(CourtLocationFilterEnum.values()),
                        Arrays.asList(CourtLocationSortEnum.values()));
        scenario.getAllKeyable().stream().forEach(nc -> nc.setCourtType("CHOA"));

        // lets set the rest endpoint
        RestFilterEndpointDescription<NationalCourtHouse> restFilterDescription =
                new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("court-locations"));
        restFilterDescription.setSortDescriptors(Arrays.asList(CourtLocationSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(
                restFilterDescription
                        .getForScenario(filterScenarioStrategy)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<NationalCourtHouse>> getSortDescriptions()
            throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);
        nationalCourtHouse.setStartDate(LocalDate.now().minusDays(1));

        // process the scenario
        List<NationalCourtHouse> nationalCourtHouses =
                FilterScenarioFactory.createSort(
                        nationalCourtHouse, Arrays.asList(CourtLocationSortEnum.values()));

        nationalCourtHouses.stream().forEach(nc -> nc.setCourtType("CHOA"));

        List<RestSortEndpointDescription<NationalCourtHouse>> sortEndpointDescriptions =
                new ArrayList<>();
        for (CourtLocationSortEnum courtLocationSortEnum : CourtLocationSortEnum.values()) {
            RestSortEndpointDescription<NationalCourtHouse> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("court-locations"));
            restFilterDescription.setSortDescriptors(courtLocationSortEnum);
            restFilterDescription.setExpectedToBeGenerated(nationalCourtHouses);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(CourtLocationSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(
            List<NationalCourtHouse> keyable, Response response, List<NationalCourtHouse> exclude) {
        CourtLocationPage page = response.as(CourtLocationPage.class);
        List<CourtLocationGetSummaryDto> content = page.getContent();

        // assert the excludes are not in the response
        assertExcluded(response, exclude);

        // assert the order of the response is correct and all keys that are expected are there
        int expectedIndex = 0;

        for (CourtLocationGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex)
                            .getCourtLocationCode()
                            .equals(item.getLocationCode())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    private void assertExcluded(Response response, List<NationalCourtHouse> exclude) {
        CourtLocationPage page = response.as(CourtLocationPage.class);
        List<CourtLocationGetSummaryDto> content = page.getContent();
        for (NationalCourtHouse keyable : exclude) {
            Assertions.assertFalse(
                    content.stream()
                            .anyMatch(
                                    dto ->
                                            dto.getLocationCode()
                                                    .equals(keyable.getCourtLocationCode())));
        }
    }

    @Override
    protected NationalCourtHouse saveToDatabase(NationalCourtHouse keyable) {
        return this.persistance.save(keyable);
    }

    private void assertKeyableForSummary(
            NationalCourtHouse keyable, CourtLocationGetSummaryDto dto) {
        Assertions.assertEquals(keyable.getCourtLocationCode(), dto.getLocationCode());
        Assertions.assertEquals(keyable.getName(), dto.getName());
    }
}
