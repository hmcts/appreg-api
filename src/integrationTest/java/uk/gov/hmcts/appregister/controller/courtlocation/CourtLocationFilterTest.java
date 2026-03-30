package uk.gov.hmcts.appregister.controller.courtlocation;

import io.restassured.response.Response;

import org.junit.jupiter.api.Assertions;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationFilterEnum;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationSortEnum;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CourtLocationFilterTest extends AbstractFilterAndSortControllerTest<NationalCourtHouse> {

    @Override
    protected Stream<RestFilterEndpointDescription<NationalCourtHouse>> getFilterDescriptions() throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);

        // process the scenario
        FilterableScenario<NationalCourtHouse> scenario
            = FilterScenarioFactory.createFilterScenario(nationalCourtHouse,
                                                         Arrays.asList(CourtLocationFilterEnum.values()),
                                                         Arrays.asList(CourtLocationSortEnum.values()));
        scenario.getAllKeyable().stream().forEach(nc -> nc.setCourtType("CHOA"));


        // lets set the rest endpoint
        RestFilterEndpointDescription<NationalCourtHouse> restFilterDescription = new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("court-locations"));
        restFilterDescription.setSortDescriptors(Arrays.asList(CourtLocationSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<NationalCourtHouse>> getSortDescriptions() throws Exception {
        // create the application code
        NationalCourtHouse nationalCourtHouse = new NationalCourtHouseData().someComplete();
        nationalCourtHouse.setEndDate(null);

        // process the scenario
        List<NationalCourtHouse> nationalCourtHouses
            = FilterScenarioFactory.createSort(nationalCourtHouse, Arrays.asList(CourtLocationSortEnum.values()));

        nationalCourtHouses.stream().forEach(nc -> nc.setCourtType("CHOA"));

        List<RestSortEndpointDescription<NationalCourtHouse>> sortEndpointDescriptions = new
            ArrayList<>();
        for (CourtLocationSortEnum courtLocationSortEnum
            : CourtLocationSortEnum.values()) {
            RestSortEndpointDescription<NationalCourtHouse> restFilterDescription = new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("court-locations"));
            restFilterDescription.setSortDescriptors(courtLocationSortEnum);
            restFilterDescription.setExpectedToBeGenerated(nationalCourtHouses);
            restFilterDescription.setAllAvailableSortDescriptors(Arrays.asList(CourtLocationSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<NationalCourtHouse> keyable, Response response) {
        CourtLocationPage page = response.as(CourtLocationPage.class);

        for (int i = 0; i < keyable.size(); i++) {
            matchSummary(page, keyable.get(i).getCourtLocationCode());
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        CourtLocationPage page = response.as(CourtLocationPage.class);
        return size == page.getContent().size();
    }

    private  void matchSummary(CourtLocationPage page, String code) {
        for (CourtLocationGetSummaryDto applicationCodeGetSummaryDto : page.getContent()) {
            if (applicationCodeGetSummaryDto.getLocationCode().equals(code)) {
                return;
            }
        }
        throw new FilterProcessingException("Code not found");
    }


    @Override
    protected NationalCourtHouse saveToDatabase(NationalCourtHouse keyable) {
        return this.persistance.save(keyable);
    }
}
