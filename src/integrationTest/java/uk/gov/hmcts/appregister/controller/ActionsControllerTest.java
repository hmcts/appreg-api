package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import static org.assertj.core.api.Assertions.assertThat;

public class ActionsControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID =
        "ffffffff-ffff-ffff-ffff-ffffffffffff";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    @Test
    @DisplayName("Move Application List Entries")
    void givenValidRequest_whenMove_then200() throws Exception {
        var token =
            getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(2),
                Optional.of(0),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                token,
                rs -> rs.header("Accept", VND_JSON_V1)
                    .queryParam("status", ApplicationListStatus.OPEN.toString()),
                null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        UUID sourceListId = page.getContent().get(0).getId();
        UUID targetListId = page.getContent().get(1).getId();

        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + sourceListId), token);
        ApplicationListGetDetailDto applicationListGetDetailDto = resp.as(ApplicationListGetDetailDto.class);

        Set<UUID> entryIds = new HashSet<>();
        UUID entry1Id = applicationListGetDetailDto.getEntriesSummary().get(0).getUuid();
        UUID entry2Id = applicationListGetDetailDto.getEntriesSummary().get(1).getUuid();
        entryIds.add(entry1Id);
        entryIds.add(entry2Id);

        var req = new MoveEntriesDto().targetListId(targetListId).entryIds(entryIds);

        // fire test
        resp =
            restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + sourceListId + "/entries/move"),
                token,
                req);

        // assert success
        resp.then().statusCode(HttpStatus.OK.value());

        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + targetListId), token);
        applicationListGetDetailDto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(applicationListGetDetailDto.getEntriesSummary())
            .extracting(ApplicationListEntrySummary::getUuid)
            .contains(entry1Id, entry2Id);
    }

    @Test
    @DisplayName("Move Application List Entries: 404 when source list unknown")
    void givenUnknownTargetApplicationList_whenMoveApplicationListEntries_then404() throws Exception {
        var token =
            getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(2),
                Optional.of(0),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                token,
                rs -> rs.header("Accept", VND_JSON_V1)
                    .queryParam("status", ApplicationListStatus.OPEN.toString()),
                null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        UUID sourceListId = page.getContent().getFirst().getId();
        UUID targetListId = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());
        var req = new MoveEntriesDto().targetListId(targetListId).entryIds(entryIds);

        // fire test
        resp =
            restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + sourceListId + "/entries/move"),
                token,
                req);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
            ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
            problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 404 when target list unknown")
    void givenUnknownSourceApplicationList_whenMoveApplicationListEntries_then404() throws Exception {
        var token =
            getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();

        UUID sourceListId = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());
        var req = new MoveEntriesDto().targetListId(UUID.randomUUID()).entryIds(entryIds);

        // fire test
        Response resp =
            restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + sourceListId + "/entries/move"),
                token,
                req);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
            ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
            problemDetail.getType().toString());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID() + "/entries/move"))
                        .method(HttpMethod.POST)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private UUID createApplicationList(TokenAndJwksKey token) throws MalformedURLException {
        String description = "List for testing move application list entries";

        var req =
                new ApplicationListCreateDto()
                        .date(LocalDate.now())
                        .time(LocalTime.now())
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().log().ifError();
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        return dto.getId();
    }
}
