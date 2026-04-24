package uk.gov.hmcts.appregister.controller.applicationentry;

import io.restassured.response.Response;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.filter.AppListEntryFeeIdMixin;
import uk.gov.hmcts.appregister.filter.ApplicationListMixin;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.NameAddressMixin;
import uk.gov.hmcts.appregister.filter.applicationlistentry.ApplicationListEntryFilterEnum;
import uk.gov.hmcts.appregister.filter.applicationlistentry.ApplicationListEntrySortEnum;
import uk.gov.hmcts.appregister.filter.applicationlistentry.ApplicationListFilterForAppListIdEnum;
import uk.gov.hmcts.appregister.filter.applicationlistentry.ApplicationListSortForAppListIdEnum;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.testutils.controller.AbstractFilterAndSortControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestFilterEndpointDescription;
import uk.gov.hmcts.appregister.testutils.controller.RestSortEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.util.CopyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ApplicationListFilterSortTest
        extends AbstractFilterAndSortControllerTest<ApplicationListEntry> {
    @Autowired private ApplicationListRepository applicationListRepository;

    @Override
    protected Stream<RestFilterEndpointDescription<ApplicationListEntry>> getFilterDescriptions()
            throws Exception {

        Jwt jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);
        CopyUtil.registerMixin(AppListEntryFeeId.class, AppListEntryFeeIdMixin.class);

        RestFilterEndpointDescription<ApplicationListEntry> restFilterDescription =
                new RestFilterEndpointDescription<>();

        // create the application code
        ApplicationListEntry applicationListEntry =
                new AppListEntryTestData().someMinimal().build();

        // process the scenario
        FilterableScenario<ApplicationListEntry> scenario =
                FilterScenarioFactory.createFilterScenario(
                        applicationListEntry,
                        Arrays.asList(ApplicationListEntryFilterEnum.values()),
                        Arrays.asList(ApplicationListEntrySortEnum.values()));

        // lets set the rest endpoint
        restFilterDescription.setFilterableScenario(scenario);
        restFilterDescription.setGetUrlFunction(
                (keyable) ->
                        getLocalUrl(
                                "application-list-entries"));
        restFilterDescription.setSortDescriptors(
                Arrays.asList(ApplicationListEntrySortEnum.values()));

        // gets all of the combinations of filters
        return Stream.of(
                restFilterDescription
                        .getForScenario(restFilterDescription.getFilterableScenario())
                        .toArray(new RestFilterEndpointDescription[0]));
    }

    @Override
    protected Stream<RestSortEndpointDescription<ApplicationListEntry>> getSortDescriptions()
            throws Exception {

        Jwt jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CopyUtil.registerMixin(NameAddress.class, NameAddressMixin.class);
        CopyUtil.registerMixin(ApplicationList.class, ApplicationListMixin.class);
        CopyUtil.registerMixin(AppListEntryFeeId.class, AppListEntryFeeIdMixin.class);

        // get all of the sort endpoint descriptions
        List<RestSortEndpointDescription<ApplicationListEntry>> sortEndpointDescriptions =
                new ArrayList<>();

        // create the list entry
        ApplicationListEntry applicationListEntry =
                new AppListEntryTestData().someMinimal().build();

        // process the scenario
        List<ApplicationListEntry> applicationListEntries =
                FilterScenarioFactory.createSort(
                        applicationListEntry,
                        Arrays.asList(ApplicationListEntrySortEnum.values()));

        for (ApplicationListEntrySortEnum applicationCodeSortEnum :
            ApplicationListEntrySortEnum.values()) {
            RestSortEndpointDescription<ApplicationListEntry> restFilterDescription =
                    new RestSortEndpointDescription<>();
            restFilterDescription.setGetUrlFunction(
                    (key) ->
                            getLocalUrl(
                                    "application-list-entries"));
            restFilterDescription.setSortDescriptors(applicationCodeSortEnum);
            restFilterDescription.setExpectedToBeGenerated(applicationListEntries);
            restFilterDescription.setAllAvailableSortDescriptors(
                    Arrays.asList(ApplicationListSortForAppListIdEnum.values()));
            sortEndpointDescriptions.add(restFilterDescription);
        }

        return Stream.of(sortEndpointDescriptions.toArray(new RestSortEndpointDescription[0]));
    }

    @Override
    protected ApplicationListEntry saveToDatabase(ApplicationListEntry keyable) {
        return this.persistance.save(keyable);
    }

    @Override
    protected boolean assertResponseInOrder(
            List<ApplicationListEntry> keyable,
            Response response,
            List<ApplicationListEntry> exclude) {
        EntryPage page = response.as(EntryPage.class);
        List<EntryGetSummaryDto> content = page.getContent();

        // assert the excludes are not in the response
        assertExcluded(response, exclude);

        // assert the order of the response is correct and all keys that are expected are there
        int expectedIndex = 0;

        for (EntryGetSummaryDto item : content) {
            if (expectedIndex < keyable.size()
                    && keyable.get(expectedIndex).getUuid().equals(item.getId())) {
                assertKeyableForSummary(keyable.get(expectedIndex), item);
                expectedIndex++;
            }
        }

        if (expectedIndex != keyable.size()) {
            throw new FilterProcessingException("Expected codes were not found in order");
        }

        return true;
    }

    /**
     * asserts that the entries in the response are not in the exclude list.
     *
     * @param exclude The entries to exclude from the response.
     */
    private void assertExcluded(Response response, List<ApplicationListEntry> exclude) {
        EntryPage page = response.as(EntryPage.class);
        List<EntryGetSummaryDto> content = page.getContent();

        for (ApplicationListEntry entry : exclude) {
            Assertions.assertFalse(
                    content.stream().anyMatch(dto -> dto.getId().equals(entry.getUuid())));
        }
    }

    private void assertKeyableForSummary(ApplicationListEntry keyable, EntryGetSummaryDto dto) {
        Assertions.assertEquals(keyable.getAccountNumber(), dto.getAccountNumber().get());
        Assertions.assertEquals(keyable.getApplicationCode().getTitle(), dto.getApplicationTitle());
        Assertions.assertEquals(keyable.getSequenceNumber().intValue(), dto.getSequenceNumber());
        Assertions.assertEquals(
                keyable.getApplicationCode().getFeeDue().isYes(), dto.getIsFeeRequired());
    }
}
