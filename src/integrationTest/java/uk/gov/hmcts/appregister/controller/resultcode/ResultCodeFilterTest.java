package uk.gov.hmcts.appregister.controller.resultcode;

import io.restassured.response.Response;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.filter.resultcode.ResultCodeFilterEnum;
import uk.gov.hmcts.appregister.data.filter.resultcode.ResultCodeSortEnum;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

public class ResultCodeFilterTest extends AbstractFilterAndSortControllerTest<ResolutionCode> {

    @Override
    protected Stream<RestFilterEndpointDescription<ResolutionCode>> getFilterDescriptions() throws Exception {
        // create the application code
        ResolutionCode resolutionCode = new ResolutionCodeTestData().someComplete();

        // process the scenario
        FilterableScenario<ResolutionCode> scenario
            = FilterScenarioFactory.createFilterScenario(resolutionCode,
                                                         Arrays.asList(ResultCodeFilterEnum.values()),
                                                         Arrays.asList(ResultCodeSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<ResolutionCode> restFilterDescription = new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("result-codes"));
        restFilterDescription.setSortDescriptors(Arrays.asList(ResultCodeSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ResolutionCode>> getSortDescriptions() throws Exception {
        // create the application code
        ResolutionCode resolutionCode = new ResolutionCodeTestData().someComplete();

        // process the scenario
        List<ResolutionCode> criminalJusticeAreas
            = FilterScenarioFactory.createSort(resolutionCode, Arrays.asList(ResultCodeSortEnum.values()));

        List<RestSortEndpointDescription<ResolutionCode>> sortEndpointDescriptions = new
            ArrayList<>();
        for (ResultCodeSortEnum resultCodeSortEnum
            : ResultCodeSortEnum.values()) {
            RestSortEndpointDescription<ResolutionCode> restFilterDescription = new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("result-codes"));
            restFilterDescription.setSortDescriptors(resultCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(criminalJusticeAreas);
            restFilterDescription.setAllAvailableSortDescriptors(Arrays.asList(ResultCodeSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<ResolutionCode> keyable, Response response) {
        ResultCodePage page = response.as(ResultCodePage.class);

        for (int i = 0; i < keyable.size(); i++) {
            matchSummary(page, keyable.get(i).getResultCode());
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        ResultCodePage page = response.as(ResultCodePage.class);
        return size == page.getContent().size();
    }

    private  void matchSummary(ResultCodePage page, String code) {
        for (ResultCodeGetSummaryDto standardApplicantGetSummaryDto : page.getContent()) {
            if (standardApplicantGetSummaryDto.getResultCode().equals(code)) {
                return;
            }
        }
        throw new FilterProcessingException("Code not found");
    }


    @Override
    protected ResolutionCode saveToDatabase(ResolutionCode keyable) {
        return this.persistance.save(keyable);
    }
}
