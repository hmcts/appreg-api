package uk.gov.hmcts.appregister.controller.applicationcode;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.applicationcode.ApplicationCodeFilterEnum;
import uk.gov.hmcts.appregister.filter.applicationcode.ApplicationCodeSortEnum;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
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

        // make sure that the fee reference actually references a fee record
        applicationCode.setFeeReference("CO3.2");

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
        restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("application-codes"));
        restFilterDescription.setSortDescriptors(Arrays.asList(ApplicationCodeSortEnum.values()));

        // gets all of the combinations of filters
        return Stream.of(
                restFilterDescription
                        .getForScenario(filterScenarioStrategy)
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ApplicationCode>> getSortDescriptions()
            throws Exception {
        // create the application code
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        // make sure that the fee reference actually references a fee record
        applicationCode.setFeeReference("CO3.2");

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
            restFilterDescription.setGetUrlFunction((key) -> getLocalUrl("application-codes"));
            restFilterDescription.setSortDescriptors(applicationCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(applicationCodes);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(ApplicationCodeSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected ApplicationCode saveToDatabase(ApplicationCode keyable) {
        return this.persistance.save(keyable);
    }

    @Override
    protected boolean assertResponseInOrder(
            List<ApplicationCode> keyable, Response response, List<ApplicationCode> exclude) {
        ApplicationCodePage page = response.as(ApplicationCodePage.class);
        List<ApplicationCodeGetSummaryDto> content = page.getContent();

        // assert excluded
        assertExcluded(response, exclude);

        // asserts included and order
        int expectedIndex = 0;

        for (ApplicationCodeGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getCode().equals(item.getApplicationCode())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    private void assertExcluded(Response response, List<ApplicationCode> exclude) {
        ApplicationCodePage page = response.as(ApplicationCodePage.class);
        List<ApplicationCodeGetSummaryDto> content = page.getContent();
        for (ApplicationCode keyable : exclude) {
            Assertions.assertFalse(
                    content.stream()
                            .anyMatch(dto -> dto.getApplicationCode().equals(keyable.getCode())));
        }
    }

    private void assertKeyableForSummary(
            ApplicationCode keyable, ApplicationCodeGetSummaryDto dto) {
        Assertions.assertEquals(keyable.getCode(), dto.getApplicationCode());
        Assertions.assertEquals(keyable.getTitle(), dto.getTitle());
        Assertions.assertEquals(
                keyable.getBulkRespondentAllowed().isYes(), dto.getBulkRespondentAllowed());
        Assertions.assertEquals(keyable.getFeeDue().isYes(), dto.getIsFeeDue());
        Assertions.assertEquals(
                keyable.getRequiresRespondent().isYes(), dto.getRequiresRespondent());
        Assertions.assertEquals(keyable.getFeeReference(), dto.getFeeReference().get());
        Assertions.assertEquals(
                WordingTemplateSentence.with(keyable.getWording()).getDetail().getTemplate(),
                dto.getWording().getTemplate());
    }
}
