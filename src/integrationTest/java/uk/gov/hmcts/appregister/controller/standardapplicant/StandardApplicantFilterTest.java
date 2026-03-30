package uk.gov.hmcts.appregister.controller.standardapplicant;

import io.restassured.response.Response;

import org.junit.jupiter.api.Assertions;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.criminaljusticearea.CriminalJusticeAreaFilterEnum;
import uk.gov.hmcts.appregister.data.filter.criminaljusticearea.CriminalJusticeAreaSortEnum;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.filter.standardapplicant.StandardApplicantFilterEnum;
import uk.gov.hmcts.appregister.data.filter.standardapplicant.StandardApplicantSortEnum;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class StandardApplicantFilterTest extends AbstractFilterAndSortControllerTest<StandardApplicant> {

    @Override
    protected Stream<RestFilterEndpointDescription<StandardApplicant>> getFilterDescriptions() throws Exception {
        // create the application code
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        // process the scenario
        FilterableScenario<StandardApplicant> scenario
            = FilterScenarioFactory.createFilterScenario(standardApplicant,
                                                         Arrays.asList(StandardApplicantFilterEnum.values()),
                                                         Arrays.asList(StandardApplicantSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<StandardApplicant> restFilterDescription = new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("standard-applicants"));
        restFilterDescription.setSortDescriptors(Arrays.asList(StandardApplicantSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<StandardApplicant>> getSortDescriptions() throws Exception {
        // create the application code
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        // process the scenario
        List<StandardApplicant> criminalJusticeAreas
            = FilterScenarioFactory.createSort(standardApplicant, Arrays.asList(StandardApplicantSortEnum.values()));

        List<RestSortEndpointDescription<StandardApplicant>> sortEndpointDescriptions = new
            ArrayList<>();
        for (StandardApplicantSortEnum standardApplicantSortEnum
            : StandardApplicantSortEnum.values()) {
            RestSortEndpointDescription<StandardApplicant> restFilterDescription = new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("standard-applicants"));
            restFilterDescription.setSortDescriptors(standardApplicantSortEnum);
            restFilterDescription.setExpectedToBeGenerated(criminalJusticeAreas);
            restFilterDescription.setAllAvailableSortDescriptors(Arrays.asList(StandardApplicantSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<StandardApplicant> keyable, Response response) {
        StandardApplicantPage page = response.as(StandardApplicantPage.class);

        for (int i = 0; i < keyable.size(); i++) {
            matchSummary(page, keyable.get(i).getApplicantCode());
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        StandardApplicantPage page = response.as(StandardApplicantPage.class);
        return size == page.getContent().size();
    }

    private  void matchSummary(StandardApplicantPage page, String code) {
        for (StandardApplicantGetSummaryDto standardApplicantGetSummaryDto : page.getContent()) {
            if (standardApplicantGetSummaryDto.getCode().equals(code)) {
                return;
            }
        }
        throw new FilterProcessingException("Code not found");
    }


    @Override
    protected StandardApplicant saveToDatabase(StandardApplicant keyable) {
        return this.persistance.save(keyable);
    }
}
