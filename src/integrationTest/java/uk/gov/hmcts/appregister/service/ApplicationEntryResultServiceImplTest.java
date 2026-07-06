package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.applicationentryresult.api.ApplicationEntryResultSortFieldEnum;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.applicationentryresult.service.ApplicationEntryResultService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.TemplateAssertion;

class ApplicationEntryResultServiceImplTest extends BaseIntegration {

    private static final String RTC_CODE = "RTC";

    @Autowired private DatabasePersistance persistance;

    @Autowired private ApplicationEntryResultService applicationEntryResultService;

    @Autowired private PageableMapper pageableMapper;

    private static final String DATE_VALUE = "a date";
    private static final String COURT_HOUSE_VALUE = "Courthouse value";

    private static final String DATE_KEY = "Date";
    private static final String COURT_HOUSE_KEY = "Courthouse";

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testBulkResultWithoutList() throws Exception {
        ApplicationList list = new AppListTestData().someMinimal().build();
        list.setStatus(OPEN);
        ApplicationListEntry entry = new AppListEntryTestData().someMinimal().build();
        entry.setApplicationList(list);

        // save the two lists we a single entry
        persistance.save(entry);

        ApplicationList list2 = new AppListTestData().someMinimal().build();
        list2.setStatus(OPEN);
        ApplicationListEntry entry2 = new AppListEntryTestData().someMinimal().build();
        entry2.setApplicationList(list2);

        persistance.save(entry2);

        BulkResultDto bulkResultDto = new BulkResultDto();

        // add entries
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)));
        bulkResultDto.setResult(createDto);

        // make a call to the service to create the result for the entry without a list
        PayloadForCreateResults<BulkResultDto> bulkPayload =
                PayloadForCreateResults.<BulkResultDto>builder().payload(bulkResultDto).build();
        applicationEntryResultService.bulkCreate(bulkPayload);

        // make another call to add another result two in total
        applicationEntryResultService.bulkCreate(bulkPayload);

        // create the paging wrapper for one page with one row
        PagingWrapper pagingWrapper =
                pageableMapper.from(
                        0,
                        2,
                        List.of(),
                        ApplicationEntryResultSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        ApplicationEntryResultSortFieldEnum::getEntityValue);

        // assert the two results on the first list entry
        PayloadGetEntryResultInList getEntryResultInList =
                PayloadGetEntryResultInList.builder()
                        .listId(list.getUuid())
                        .entryId(entry.getUuid())
                        .build();
        ResultPage resultPage =
                applicationEntryResultService.search(getEntryResultInList, pagingWrapper);

        Assertions.assertEquals(2, resultPage.getPageSize());
        Assertions.assertEquals(2, resultPage.getTotalElements());
        Assertions.assertEquals(RTC_CODE, resultPage.getContent().get(0).getResultCode());
        Assertions.assertEquals(entry.getUuid(), resultPage.getContent().get(0).getEntryId());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)),
                resultPage.getContent().get(0).getWording());
        Assertions.assertEquals(RTC_CODE, resultPage.getContent().get(1).getResultCode());
        Assertions.assertEquals(entry.getUuid(), resultPage.getContent().get(1).getEntryId());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)),
                resultPage.getContent().get(0).getWording());

        // assert the two results on the second list entry
        getEntryResultInList =
                PayloadGetEntryResultInList.builder()
                        .listId(list2.getUuid())
                        .entryId(entry2.getUuid())
                        .build();
        resultPage = applicationEntryResultService.search(getEntryResultInList, pagingWrapper);

        Assertions.assertEquals(entry2.getUuid(), resultPage.getContent().get(0).getEntryId());
        Assertions.assertEquals(RTC_CODE, resultPage.getContent().get(0).getResultCode());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)),
                resultPage.getContent().get(0).getWording());

        Assertions.assertEquals(entry2.getUuid(), resultPage.getContent().get(1).getEntryId());
        Assertions.assertEquals(RTC_CODE, resultPage.getContent().get(1).getResultCode());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)),
                resultPage.getContent().get(1).getWording());
    }

    @Test
    void testEntryDoesNotExistFailure() throws Exception {
        ApplicationList list = new AppListTestData().someMinimal().build();
        list.setStatus(OPEN);
        ApplicationListEntry entry = new AppListEntryTestData().someMinimal().build();
        entry.setApplicationList(list);

        // save the two lists we a single entry
        persistance.save(entry);

        ApplicationList list2 = new AppListTestData().someMinimal().build();
        list2.setStatus(OPEN);
        ApplicationListEntry entry2 = new AppListEntryTestData().someMinimal().build();
        entry2.setApplicationList(list2);

        persistance.save(entry2);

        BulkResultDto bulkResultDto = new BulkResultDto();

        // add an entry that does not exist
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid(), UUID.randomUUID()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution(DATE_KEY, DATE_VALUE),
                        new TemplateSubstitution(COURT_HOUSE_KEY, COURT_HOUSE_VALUE)));
        bulkResultDto.setResult(createDto);

        // make a call to the service to create the result for the entry without a list
        PayloadForCreateResults<BulkResultDto> bulkPayload =
                PayloadForCreateResults.<BulkResultDto>builder().payload(bulkResultDto).build();
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            applicationEntryResultService.bulkCreate(bulkPayload);
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST
                        .getCode()
                        .getType(),
                appRegistryException.getCode().getCode().getType());
    }
}
