package uk.gov.hmcts.appregister.controller.applicationentryresult;

import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.audit.event.OperationStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationEntryResultControllerSearchTest
        extends AbstractApplicationEntryResultCrudTest {

    @StabilityTest
    public void givenApplicationListEntryResult_whenSearchForResults_thenSuccessResponse()
            throws Exception {
        UUID appList =
                UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createAppList(null)[0]));
        EntryGetDetailDto detailDto = createEntry(appList).getDetailDto();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // create results
        for (int i = 0; i < 4; i++) {
            ResultCreateDto createDto = new ResultCreateDto();

            if (i % 2 == 0) {
                createDto.setResultCode("CASE");
                Response response = createResult(appList, detailDto.getId(), token, createDto);

                Assertions.assertEquals(201, response.getStatusCode());
            } else {
                TemplateSubstitution substitution = new TemplateSubstitution();
                substitution.setKey("Name of Crown Court");
                substitution.setValue("test wording " + i);

                createDto.setResultCode("APPC");
                createDto.setWordingFields(List.of(substitution));

                // create a result
                Response response = createResult(appList, detailDto.getId(), token, createDto);

                Assertions.assertEquals(201, response.getStatusCode());
            }
        }

        // navigate to the second page
        Response response = getEntryResult(token, appList, detailDto.getId(), 1, 1);

        Assertions.assertEquals(200, response.getStatusCode());
        ResultPage page = response.as(ResultPage.class);
        Assertions.assertEquals(1, page.getContent().size());

        Assertions.assertEquals("APPC", page.getContent().get(0).getResultCode());
        Assertions.assertEquals(
                "Appeal forwarded to {{Name of Crown Court}}.",
                page.getContent().get(0).getWording().getTemplate());
        Assertions.assertEquals(
                "test wording 3",
                page.getContent()
                        .get(0)
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getValue());
        Assertions.assertEquals(
                "Name of Crown Court",
                page.getContent()
                        .get(0)
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getKey());

        // get the next page of data
        response = getEntryResult(token, appList, detailDto.getId(), 1, 2);

        Assertions.assertEquals(200, response.getStatusCode());
        page = response.as(ResultPage.class);
        Assertions.assertEquals("CASE", page.getContent().get(0).getResultCode());
        Assertions.assertEquals(
                "Court agrees to state a case for the opinion of the High Court.",
                page.getContent().get(0).getWording().getTemplate());
        Assertions.assertEquals(
                0, page.getContent().get(0).getWording().getSubstitutionKeyConstraints().size());

        // assert the data audit
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "id",
                        null,
                        appList.toString(),
                        CrudEnum.READ.name(),
                        AppListEntryResultAuditOperation.GET_APP_LIST_ENTRY_RESULT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        null,
                        detailDto.getId().toString(),
                        CrudEnum.READ.name(),
                        AppListEntryResultAuditOperation.GET_APP_LIST_ENTRY_RESULT.getEventName()));

        // assert the the activity log is entered
        activityAuditLogAsserter.assertCompletedLogContains(
                AppListEntryResultAuditOperation.GET_APP_LIST_ENTRY_RESULT.getEventName(),
                RestAssuredClient.DEFAULT_TRACE_ID,
                Integer.valueOf(OperationStatus.COMPLETED.getStatus()).toString(),
                mapper.writeValueAsString(page));
    }

    @Test
    public void
            givenApplicationListEntryResult_whenSearchForResultsWithClosedList_thenSuccessResponse()
                    throws Exception {
        // create 20 results
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Name of Crown Court");
        substitution.setValue("test wording");

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode("APPC");
        createDto.setWordingFields(List.of(substitution));

        UUID appList =
                UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createAppList(null)[0]));

        EntryGetDetailDto detailDto = createEntry(appList).getDetailDto();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // create a result
        Response response = createResult(appList, detailDto.getId(), token, createDto);

        Assertions.assertEquals(201, response.getStatusCode());

        unitOfWork.inTransaction(
                () -> {
                    // set the list to closed to prove this does not affect the get
                    ApplicationList applicationList =
                            applicationListRepository.findByUuid(appList).get();
                    applicationList.setStatus(Status.CLOSED);
                    persistance.save(applicationList);
                });

        // navigate to the second page
        response = getEntryResult(token, appList, detailDto.getId(), 10, 0);

        Assertions.assertEquals(200, response.getStatusCode());
        ResultPage page = response.as(ResultPage.class);
        Assertions.assertEquals(1, page.getContent().size());

        Assertions.assertEquals("APPC", page.getContent().get(0).getResultCode());
        Assertions.assertEquals(
                "Appeal forwarded to {{Name of Crown Court}}.",
                page.getContent().get(0).getWording().getTemplate());
        Assertions.assertEquals(
                "test wording",
                page.getContent()
                        .get(0)
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getValue());
        Assertions.assertEquals(
                "Name of Crown Court",
                page.getContent()
                        .get(0)
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getKey());
    }

    @Test
    public void givenApplicationListEntryResult_whenSearchForResultsWithNoList_thenFailureResponse()
            throws Exception {
        UUID appList = UUID.randomUUID();

        // create an entry
        UUID detailDto = UUID.randomUUID();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // navigate to the second page
        Response response = getEntryResult(token, appList, detailDto, 10, 0);

        Assertions.assertEquals(409, response.getStatusCode());
        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(),
                response);
    }

    @Test
    public void
            givenApplicationListEntryResult_whenSearchForResultsWithNoEntry_thenFailureResponse()
                    throws Exception {
        UUID appList =
                UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createAppList(null)[0]));

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // navigate to the second page
        Response response = getEntryResult(token, appList, UUID.randomUUID(), 10, 0);

        Assertions.assertEquals(409, response.getStatusCode());
        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(),
                response);
    }

    @Test
    public void givenApplicationListEntryResult_whenApplicationlistDeleted_thenFailureResponse()
            throws Exception {
        // create 20 results
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Name of Crown Court");
        substitution.setValue("test wording");

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode("APPC");
        createDto.setWordingFields(List.of(substitution));

        UUID appList =
                UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createAppList(null)[0]));

        EntryGetDetailDto detailDto = createEntry(appList).getDetailDto();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // create a result
        Response response = createResult(appList, detailDto.getId(), token, createDto);

        Assertions.assertEquals(201, response.getStatusCode());

        unitOfWork.inTransaction(
                () -> {
                    // set the list to deleted
                    ApplicationList applicationList =
                            applicationListRepository.findByUuid(appList).get();
                    applicationList.setDeleted(YesOrNo.YES);
                    persistance.save(applicationList);
                });

        // navigate to the second page
        Response actualResponse = getEntryResult(token, appList, detailDto.getId(), 10, 0);

        Assertions.assertEquals(409, actualResponse.getStatusCode());
        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(),
                actualResponse);
    }

    @Test
    public void
            givenApplicationListEntryResult_whenApplicationlistEntryDeleted_thenFailureResponse()
                    throws Exception {
        // create 20 results
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Name of Crown Court");
        substitution.setValue("test wording");

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode("APPC");
        createDto.setWordingFields(List.of(substitution));

        UUID appList =
                UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createAppList(null)[0]));

        EntryGetDetailDto detailDto = createEntry(appList).getDetailDto();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // create a result
        Response response = createResult(appList, detailDto.getId(), token, createDto);

        Assertions.assertEquals(201, response.getStatusCode());

        // make the entry deleted
        unitOfWork.inTransaction(
                () -> {
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository.findByUuid(detailDto.getId()).get();
                    applicationListEntry.setDeleted(YesOrNo.YES);
                    persistance.save(applicationListEntry);
                });

        // navigate to the second page
        Response actualResponse = getEntryResult(token, appList, detailDto.getId(), 10, 0);

        Assertions.assertEquals(409, actualResponse.getStatusCode());
        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(),
                actualResponse);
    }
}
