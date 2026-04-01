package uk.gov.hmcts.appregister.controller.standardapplicant;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.filter.standardapplicant.StandardApplicantFilterEnum;
import uk.gov.hmcts.appregister.data.filter.standardapplicant.StandardApplicantSortEnum;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.ApplicantAssertion;

public class StandardApplicantFilterAndSortTest
        extends AbstractFilterAndSortControllerTest<StandardApplicant> {

    @Override
    protected Stream<RestFilterEndpointDescription<StandardApplicant>> getFilterDescriptions()
            throws Exception {
        // create the application code
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        // process the scenario
        FilterableScenario<StandardApplicant> scenario =
                FilterScenarioFactory.createFilterScenario(
                        standardApplicant,
                        Arrays.asList(StandardApplicantFilterEnum.values()),
                        Arrays.asList(StandardApplicantSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<StandardApplicant> restFilterDescription =
                new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("standard-applicants"));
        restFilterDescription.setSortDescriptors(Arrays.asList(StandardApplicantSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(
                restFilterDescription
                        .getForScenario(scenario)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<StandardApplicant>> getSortDescriptions()
            throws Exception {
        // create the application code
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        // process the scenario
        List<StandardApplicant> criminalJusticeAreas =
                FilterScenarioFactory.createSort(
                        standardApplicant, Arrays.asList(StandardApplicantSortEnum.values()));

        List<RestSortEndpointDescription<StandardApplicant>> sortEndpointDescriptions =
                new ArrayList<>();
        for (StandardApplicantSortEnum standardApplicantSortEnum :
                StandardApplicantSortEnum.values()) {
            RestSortEndpointDescription<StandardApplicant> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("standard-applicants"));
            restFilterDescription.setSortDescriptors(standardApplicantSortEnum);
            restFilterDescription.setExpectedToBeGenerated(criminalJusticeAreas);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(StandardApplicantSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<StandardApplicant> keyable, Response response) {
        StandardApplicantPage page = response.as(StandardApplicantPage.class);
        List<StandardApplicantGetSummaryDto> content = page.getContent();

        int expectedIndex = 0;

        for (StandardApplicantGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getApplicantCode().equals(item.getCode())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
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
        StandardApplicantPage page = response.as(StandardApplicantPage.class);
        return size == page.getContent().size();
    }

    private void assertKeyableForSummary(
            StandardApplicant keyable, StandardApplicantGetSummaryDto dto) {
        Assertions.assertEquals(keyable.getApplicantCode(), dto.getCode());
        Assertions.assertEquals(keyable.getApplicantStartDate(), dto.getStartDate());
        Assertions.assertEquals(keyable.getApplicantEndDate(), dto.getEndDate().get());
        if (keyable.getName() == null) {
            ApplicantAssertion.validatePerson(dto.getApplicant().getPerson(), keyable);
        } else {
            ApplicantAssertion.validateOrganisation(dto.getApplicant().getOrganisation(), keyable);
        }
    }

    @Override
    protected StandardApplicant saveToDatabase(StandardApplicant keyable) {
        return this.persistance.save(keyable);
    }
}
