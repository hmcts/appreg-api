package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;

import org.springframework.http.HttpStatus;

import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

public class ActionsControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";

    @Test
    @DisplayName("Move Application List Entries")
    void givenValidRequest_whenMove_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.fromString("25e6928c-1d73-4771-97a9-e2321678f485"));
        entryIds.add(UUID.fromString("20c37b52-4092-40f9-8270-69b96414f5c7"));

        var req =
            new MoveEntriesDto()
                .targetListId(UUID.fromString("cb8ae4d6-190d-4339-946b-894af73bbc5a"))
                .entryIds(entryIds);

        // fire test
        Response resp = restAssuredClient.executePostRequest(
            getLocalUrl(WEB_CONTEXT +
                            "/" + UUID.fromString("6d9d57cb-53f3-414e-8c68-c0d4b852deb3") + "/entries/move"
            ),
            token,
            req
        );

        // assert success
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
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
}
