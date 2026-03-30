package uk.gov.hmcts.appregister.controller.criminaljustice;

import io.restassured.response.Response;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationFilterEnum;
import uk.gov.hmcts.appregister.data.filter.courtlocation.CourtLocationSortEnum;
import uk.gov.hmcts.appregister.data.filter.criminaljusticearea.CriminalJusticeAreaFilterEnum;
import uk.gov.hmcts.appregister.data.filter.criminaljusticearea.CriminalJusticeAreaSortEnum;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

public class CriminalJusticeAreaFilterTest extends AbstractFilterAndSortControllerTest<CriminalJusticeArea> {

    @Override
    protected Stream<RestFilterEndpointDescription<CriminalJusticeArea>> getFilterDescriptions() throws Exception {
        // create the application code
        CriminalJusticeArea criminalJusticeArea = new CriminalJusticeTestData().someComplete();

        // process the scenario
        FilterableScenario<CriminalJusticeArea> scenario
            = FilterScenarioFactory.createFilterScenario(criminalJusticeArea,
                                                         Arrays.asList(CriminalJusticeAreaFilterEnum.values()),
                                                         Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<CriminalJusticeArea> restFilterDescription = new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("criminal-justice-areas"));
        restFilterDescription.setSortDescriptors(Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<CriminalJusticeArea>> getSortDescriptions() throws Exception {
        // create the application code
        CriminalJusticeArea nationalCourtHouse = new CriminalJusticeTestData().someComplete();

        // process the scenario
        List<CriminalJusticeArea> criminalJusticeAreas
            = FilterScenarioFactory.createSort(nationalCourtHouse, Arrays.asList(CriminalJusticeAreaSortEnum.values()));

        List<RestSortEndpointDescription<CriminalJusticeArea>> sortEndpointDescriptions = new
            ArrayList<>();
        for (CriminalJusticeAreaSortEnum criminalJusticeAreaSortEnum
            : CriminalJusticeAreaSortEnum.values()) {
            RestSortEndpointDescription<CriminalJusticeArea> restFilterDescription = new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("criminal-justice-areas"));
            restFilterDescription.setSortDescriptors(criminalJusticeAreaSortEnum);
            restFilterDescription.setExpectedToBeGenerated(criminalJusticeAreas);
            restFilterDescription.setAllAvailableSortDescriptors(Arrays.asList(CriminalJusticeAreaSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<CriminalJusticeArea> keyable, Response response) {
        CriminalJusticeAreaPage page = response.as(CriminalJusticeAreaPage.class);

        for (int i = 0; i < keyable.size(); i++) {
            matchSummary(page, keyable.get(i).getCode());
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        CriminalJusticeAreaPage page = response.as(CriminalJusticeAreaPage.class);
        return size == page.getContent().size();
    }

    private  void matchSummary(CriminalJusticeAreaPage page, String code) {
        for (CriminalJusticeAreaGetDto criminalJusticeAreaGetDto : page.getContent()) {
            if (criminalJusticeAreaGetDto.getCode().equals(code)) {
                return;
            }
        }
        throw new FilterProcessingException("Code not found");
    }


    @Override
    protected CriminalJusticeArea saveToDatabase(CriminalJusticeArea keyable) {
        return this.persistance.save(keyable);
    }
}
