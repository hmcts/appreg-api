package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ApplicationEntryControllerBulkOfficialsTest extends AbstractApplicationEntryCrudTest {

    @Test
    void givenValidEntries_whenReplaceOfficials_thenOfficialsAreReplacedForEveryEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto firstEntry =
                createEntry(List.of(official("Mr", "Original", "One", OfficialType.CLERK)));
        EntryGetDetailDto secondEntry =
                createEntry(List.of(official("Mrs", "Original", "Two", OfficialType.MAGISTRATE)));
        UUID listId = firstEntry.getListId();

        List<Official> replacementOfficials =
                List.of(
                        official("Ms", "Ada", "Bench", OfficialType.MAGISTRATE),
                        official("Mr", "Clive", "Court", OfficialType.CLERK));

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(firstEntry.getId(), secondEntry.getId()))
                                .officials(replacementOfficials));

        response.then().statusCode(204);

        assertThat(getEntry(tokenGenerator, listId, firstEntry.getId()).getOfficials())
                .containsExactlyElementsOf(replacementOfficials);
        assertThat(getEntry(tokenGenerator, listId, secondEntry.getId()).getOfficials())
                .containsExactlyElementsOf(replacementOfficials);
    }

    @Test
    void givenMissingEntry_whenReplaceOfficials_thenReturns400AndDoesNotReplaceAnyOfficials()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        List<Official> originalOfficials =
                List.of(official("Mr", "Original", "Rollback", OfficialType.CLERK));
        EntryGetDetailDto entry = createEntry(originalOfficials);
        UUID listId = entry.getListId();

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entry.getId(), UUID.randomUUID()))
                                .officials(
                                        List.of(
                                                official(
                                                        "Ms",
                                                        "Replacement",
                                                        "Blocked",
                                                        OfficialType.MAGISTRATE))));

        response.then().statusCode(400);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST.getCode().getAppCode());
        assertThat(problemDetail.getStatus()).isEqualTo(400);
        assertThat(problemDetail.getTitle()).isEqualTo("Application list entry not in source list");
        assertThat(problemDetail.getDetail()).contains("invalid_entry_ids");

        assertThat(getEntry(tokenGenerator, listId, entry.getId()).getOfficials())
                .containsExactlyElementsOf(originalOfficials);
    }

    @Test
    void givenMissingApplicationList_whenReplaceOfficials_thenReturns409() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        UUID missingListId = UUID.randomUUID();

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        missingListId,
                        validBulkOfficialsUpdateDto(UUID.randomUUID()));

        response.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), response);
    }

    @Test
    void givenClosedApplicationList_whenReplaceOfficials_thenReturns409() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        UUID closedListId = getClosedApplicationListId();

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        closedListId,
                        validBulkOfficialsUpdateDto(UUID.randomUUID()));

        response.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(), response);
    }

    @Test
    void givenEmptyEntryIds_whenReplaceOfficials_thenReturns400() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        getOpenApplicationListId(),
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of())
                                .officials(
                                        List.of(
                                                official(
                                                        "Ms",
                                                        "Ada",
                                                        "Bench",
                                                        OfficialType.MAGISTRATE))));

        response.then().statusCode(400);
    }

    @Test
    void givenMissingOfficialType_whenReplaceOfficials_thenReturns400() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(List.of(official("Mr", "Original", "MissingType", OfficialType.CLERK)));

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entry.getId()))
                                .officials(List.of(official("Ms", "No", "Type", null))));

        response.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.OFFICIAL_TYPE_REQUIRED.getCode(), response);
    }

    @Test
    void givenTooManyMagistrates_whenReplaceOfficials_thenReturns400() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(List.of(official("Mr", "Original", "Magistrates", OfficialType.CLERK)));

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entry.getId()))
                                .officials(
                                        List.of(
                                                official(
                                                        "Ms",
                                                        "Maya",
                                                        "One",
                                                        OfficialType.MAGISTRATE),
                                                official(
                                                        "Mr",
                                                        "Miles",
                                                        "Two",
                                                        OfficialType.MAGISTRATE),
                                                official(
                                                        "Mrs",
                                                        "Mina",
                                                        "Three",
                                                        OfficialType.MAGISTRATE),
                                                official(
                                                        "Mr",
                                                        "Marco",
                                                        "Four",
                                                        OfficialType.MAGISTRATE))));

        response.then().statusCode(400);
        ProblemAssertUtil.assertEquals(AppListEntryError.TOO_MANY_MAGISTRATES.getCode(), response);
    }

    @Test
    void givenTooManyCourtOfficials_whenReplaceOfficials_thenReturns400() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        List.of(
                                official(
                                        "Mr",
                                        "Original",
                                        "CourtOfficials",
                                        OfficialType.MAGISTRATE)));

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entry.getId()))
                                .officials(
                                        List.of(
                                                official(
                                                        "Mr",
                                                        "Chris",
                                                        "CourtOne",
                                                        OfficialType.CLERK),
                                                official(
                                                        "Mrs",
                                                        "Clare",
                                                        "CourtTwo",
                                                        OfficialType.CLERK))));

        response.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.TOO_MANY_COURT_OFFICIALS.getCode(), response);
    }

    @Test
    void givenUnknownOfficialType_whenReplaceOfficials_thenReturnsHelpfulEnumMessage()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(List.of(official("Mr", "Original", "UnknownType", OfficialType.CLERK)));
        String payload =
                """
                {
                  "entryIds": [
                    "%s"
                  ],
                  "officials": [
                    {
                      "title": "Ms",
                      "forename": "Invalid",
                      "surname": "Official",
                      "type": "JUDGE"
                    }
                  ]
                }
                """
                        .formatted(entry.getId());

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + entry.getListId()
                                        + "/entries/officials"),
                        tokenGenerator.fetchTokenForRole(),
                        payload);

        response.then().statusCode(400);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getDetail())
                .isEqualTo(
                        "Problem setting value for officials[0].type. Accepted values are: MAGISTRATE, CLERK");
    }

    @Test
    void givenDuplicateEntryIds_whenReplaceOfficials_thenReturns400() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        List.of(official("Mr", "Original", "DuplicateIds", OfficialType.CLERK)));

        Response response =
                replaceOfficials(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entry.getId(), entry.getId()))
                                .officials(
                                        List.of(
                                                official(
                                                        "Ms",
                                                        "Ada",
                                                        "Bench",
                                                        OfficialType.MAGISTRATE))));

        response.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE.getCode(), response);
    }

    private EntryGetDetailDto createEntry(List<Official> officials) throws Exception {
        Response response = createListEntryWithAllData(dto -> dto.setOfficials(officials));
        response.then().statusCode(201);
        return response.as(EntryGetDetailDto.class);
    }

    private Response replaceOfficials(
            TokenGenerator tokenGenerator, UUID listId, BulkOfficialsUpdateDto dto)
            throws Exception {
        return restAssuredClient.executePostRequest(
                getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/officials"),
                tokenGenerator.fetchTokenForRole(),
                dto);
    }

    private EntryGetDetailDto getEntry(TokenGenerator tokenGenerator, UUID listId, UUID entryId)
            throws Exception {
        Response response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/" + entryId),
                        tokenGenerator.fetchTokenForRole());
        response.then().statusCode(200);
        return response.as(EntryGetDetailDto.class);
    }

    private BulkOfficialsUpdateDto validBulkOfficialsUpdateDto(UUID entryId) {
        return new BulkOfficialsUpdateDto()
                .entryIds(List.of(entryId))
                .officials(List.of(official("Ms", "Ada", "Bench", OfficialType.MAGISTRATE)));
    }

    private static Official official(
            String title, String forename, String surname, OfficialType type) {
        return new Official().title(title).forename(forename).surname(surname).type(type);
    }
}
