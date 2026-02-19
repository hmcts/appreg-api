package uk.gov.hmcts.appregister.controller.applicationentryresult;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class ApplicationEntryResultControllerSecurityTest extends AbstractSecurityControllerTest {

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        "application-lists/"
                                                + listId
                                                + "/entries/"
                                                + entryId
                                                + "/results"))
                        .method(HttpMethod.POST)
                        .payload(
                                Map.of(
                                        "resultCode",
                                        "SOME_CODE",
                                        "resolutionWording",
                                        "Some wording"))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        "application-lists/"
                                                + listId
                                                + "/entries/"
                                                + entryId
                                                + "/results/"
                                                + resultId))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        "application-lists/"
                                                + listId
                                                + "/entries/"
                                                + entryId
                                                + "/results/"
                                                + resultId))
                        .method(HttpMethod.PUT)
                        .payload(Map.of("resultCode", "SOME_CODE", "wordingFields", List.of()))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
