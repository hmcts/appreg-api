package uk.gov.hmcts.appregister.controller.applicationlist;

import io.restassured.response.Response;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.applicationcode.ApplicationCodeFilterEnum;
import uk.gov.hmcts.appregister.data.filter.applicationcode.ApplicationCodeSortEnum;
import uk.gov.hmcts.appregister.data.filter.applicationlist.ApplicationListFilterEnum;
import uk.gov.hmcts.appregister.data.filter.applicationlist.ApplicationListMixin;
import uk.gov.hmcts.appregister.data.filter.applicationlist.ApplicationListSortEnum;
import uk.gov.hmcts.appregister.data.filter.applicationlist.NameAddressMixin;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import uk.gov.hmcts.appregister.util.CopyUtil;

public class ApplicationListFilterTest extends AbstractFilterAndSortControllerTest<ApplicationList> {

    @Autowired
    private EntityManager entityManager;

    @Override
    protected Stream<RestFilterEndpointDescription<ApplicationList>> getFilterDescriptions() throws Exception {
        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);

        // create the application list
        ApplicationList applicationCode = new AppListTestData().someComplete();
        applicationCode.setEntries(Set.of());

        // process the scenario
        FilterableScenario<ApplicationList> scenario
            = FilterScenarioFactory.createFilterScenario(applicationCode,
                                                         Arrays.asList(ApplicationListFilterEnum.values()),
                                                         Arrays.asList(ApplicationListSortEnum.values()));

        // lets set the rest endpoint
        RestFilterEndpointDescription<ApplicationList> restFilterDescription = new RestFilterEndpointDescription<>();
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setUrl(getLocalUrl("application-lists"));
        restFilterDescription.setSortDescriptors(Arrays.asList(ApplicationListSortEnum.values()));

        // gets all of the combinations of filters based on the start data
        return Stream.of(restFilterDescription.getForScenario(scenario)
                             .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ApplicationList>> getSortDescriptions() throws Exception {
        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);

        // create the application code
        ApplicationList applicationCode = new AppListTestData().someComplete();
        applicationCode.setEntries(Set.of());

        // process the scenario
        List<ApplicationList> applicationCodes
            = FilterScenarioFactory.createSort(applicationCode, Arrays.asList(ApplicationListSortEnum.values()));

        List<RestSortEndpointDescription<ApplicationList>> sortEndpointDescriptions = new
            ArrayList<>();
        for (ApplicationListSortEnum applicationCodeSortEnum
            : ApplicationListSortEnum.values()) {
            RestSortEndpointDescription<ApplicationList> restFilterDescription = new RestSortEndpointDescription<>();
            restFilterDescription.setUrl(getLocalUrl("application-lists"));
            restFilterDescription.setSortDescriptors(applicationCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(applicationCodes);
            restFilterDescription.setAllAvailableSortDescriptors(Arrays.asList(ApplicationListSortEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected boolean assertResponseInOrder(List<ApplicationList> keyable, Response response) {
        ApplicationListPage page = response.as(ApplicationListPage.class);

        for (int i = 0; i < keyable.size(); i++) {
            matchSummary(page, keyable.get(i).getUuid());
        }

        return true;
    }

    @Override
    protected boolean assertPageSize(int size, Response response) {
        ApplicationListPage page = response.as(ApplicationListPage.class);
        return size == page.getContent().size();
    }

    private  void matchSummary(ApplicationListPage page, UUID id) {
        for (ApplicationListGetSummaryDto applicationCodeGetSummaryDto : page.getContent()) {
            if (applicationCodeGetSummaryDto.getId().equals(id)) {
                return;
            }
        }
        throw new FilterProcessingException("Application list not found");
    }


    @Override
    protected ApplicationList saveToDatabase(ApplicationList keyable) {
        return this.persistance.save(keyable);
    }
}
