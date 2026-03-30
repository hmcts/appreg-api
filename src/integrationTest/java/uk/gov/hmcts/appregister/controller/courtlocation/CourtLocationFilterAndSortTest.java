package uk.gov.hmcts.appregister.controller.courtlocation;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationFilterEnum;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationSortEnum;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

public class CourtLocationFilterAndSortTest
        extends AbstractFilterAndSortControllerTest<NationalCourtHouse> {

    @Override
    protected Stream<RestFilterEndpointDescription<NationalCourtHouse>> getFilterDescriptions()
            throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);

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
        restFilterDescription.setUrl(getLocalUrl("court-locations"));
        restFilterDescription.setSortDescriptors(Arrays.asList(CourtLocationSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(
                restFilterDescription
                        .getForScenario(scenario)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<NationalCourtHouse>> getSortDescriptions()
            throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);

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
            restFilterDescription.setUrl(getLocalUrl("court-locations"));
            restFilterDescription.setSortDescriptors(courtLocationSortEnum);
            restFilterDescription.setExpectedToBeGenerated(nationalCourtHouses);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(CourtLocationSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<NationalCourtHouse> keyable, Response response) {
        CourtLocationPage page = response.as(CourtLocationPage.class);
        List<CourtLocationGetSummaryDto> content = page.getContent();

        int expectedIndex = 0;

        for (CourtLocationGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex)
                            .getCourtLocationCode()
                            .equals(item.getLocationCode())) {
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        CourtLocationPage page = response.as(CourtLocationPage.class);
        return size == page.getContent().size();
    }

    @Override
    protected NationalCourtHouse saveToDatabase(NationalCourtHouse keyable) {
        return this.persistance.save(keyable);
    }
}
