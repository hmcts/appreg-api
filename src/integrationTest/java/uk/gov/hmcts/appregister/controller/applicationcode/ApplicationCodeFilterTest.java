package uk.gov.hmcts.appregister.controller.applicationcode;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.filter.ApplicationCodeFilterEnum;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterDescription;

import java.net.URL;
import java.util.stream.Stream;

public class ApplicationCodeFilterTest extends AbstractFilterAndSortControllerTest {

    @Override
    protected Stream<RestFilterDescription> getFilterSortableDescriptions() throws Exception {
        // create the application code
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        // process the scenario
        FilterableScenario scenario
            = FilterScenarioFactory.createFilterScenario(applicationCode, ApplicationCodeFilterEnum.values());

        RestFilterDescription restFilterDescription = RestFilterDescription.builder()
            .filterableScenario(scenario).build();
        restFilterDescription.setUrl(new URL("/application-codes"));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterDescription[0]));
    }

    @Override
    protected String mapToResponse(Keyable keyable) {
    //    ApplicationCodeGetSummaryDto applicationCodeGetSummaryDto = new ApplicationCodeGetSummaryDto();
  //      ApplicationCodePage applicationCodePage = new ApplicationCodePage();
//        applicationCodePage.getContent().add(applicationCodeGetSummaryDto);

        return "";
    }
}
