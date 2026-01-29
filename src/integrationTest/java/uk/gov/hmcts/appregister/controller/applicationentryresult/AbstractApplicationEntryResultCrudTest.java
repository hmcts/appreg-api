package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

public abstract class AbstractApplicationEntryResultCrudTest extends BaseIntegration {

    @MockitoBean protected UserProvider provider;

    @Autowired protected AppListEntryResolutionRepository appListEntryResolutionRepository;
    @Autowired protected ResolutionCodeRepository resolutionCodeRepository;

    protected static final String WEB_CONTEXT = "application-lists";

    protected static final String APPC_CODE = "APPC";
    protected static final String APPC_WORDING_KEY = "Name of Crown Court";

    protected static final String FRO_CODE = "FRO";
    protected static final String FRO_WORDING_KEY = "Reason text";

    @BeforeEach
    void setupUser() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    // ---- HTTP helpers ----
    protected Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token)
        throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
            getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results/" + resultId),
            token);
    }

    protected Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token, String ifMatch)
        throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
            getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results/" + resultId),
            token,
            ifMatch);
    }

    protected Response createResult(UUID listId, UUID entryId, TokenAndJwksKey token, Object body)
        throws MalformedURLException {
        return restAssuredClient.executePostRequest(
            getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results"),
            token,
            body);
    }

    protected Response updateResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token, Object body, String ifMatch)
        throws MalformedURLException {
        return restAssuredClient.executePutRequest(
            getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results/" + resultId),
            token,
            body,
            ifMatch);
    }

    // ---- data helpers ----
    protected ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData().someMinimal().status(status).build();
        persistance.save(list);
        return list;
    }

    protected ApplicationListEntry createEntry(ApplicationList list) {
        return new AppListEntryTestData().someMinimal().applicationList(list).build();
    }

    protected AppListEntryResolution createAndSaveResolution(ApplicationListEntry entry,
                                                             ResolutionCode resolutionCode) {
        var resolution =
            new AppListEntryResolutionTestData()
                .someMinimal()
                .resolutionWording(AppListEntryResolutionTestData.WORDING_1)
                .applicationList(entry)
                .resolutionCode(resolutionCode)
                .build();
        return persistance.save(resolution);
    }

    protected ResultCreateDto buildCreatePayload(String resultCode, List<TemplateSubstitution> wordingFields) {
        return new ResultCreateDto(resultCode, wordingFields);
    }

    protected ResultUpdateDto buildUpdatePayload(String resultCode, List<TemplateSubstitution> wordingFields) {
        return new ResultUpdateDto(resultCode, wordingFields);
    }

    protected ResolutionCode saveActiveResolutionCode(String code, LocalDate start, LocalDate end) {
        ResolutionCode rc = new ResolutionCodeTestData().someComplete();
        rc.setResultCode(code);
        rc.setStartDate(start);
        rc.setEndDate(end);
        return persistance.save(rc);
    }

    // ---- context helpers ----

    protected record ExistingEntryContext(
        ApplicationList list,
        ApplicationListEntry entry,
        TokenAndJwksKey token) {}

    protected record ExistingEntryResultContext(
        ApplicationList list,
        ApplicationListEntry entry,
        AppListEntryResolution entryResult,
        TokenAndJwksKey token) {}

    protected ExistingEntryContext givenExistingEntry() throws Exception {
        var list = createAndSaveList(Status.OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var token = getToken();
        return new ExistingEntryContext(list, entry, token);
    }

    protected ExistingEntryResultContext givenExistingEntryResult() throws Exception {
        return givenExistingEntryResult(Status.OPEN);
    }

    protected ExistingEntryResultContext givenExistingEntryResult(Status status) throws Exception {
        var list = createAndSaveList(status);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        resolutionCode.setResultCode(APPC_CODE);
        persistance.save(resolutionCode);

        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();
        return new ExistingEntryResultContext(list, entry, entryResult, token);
    }
}
