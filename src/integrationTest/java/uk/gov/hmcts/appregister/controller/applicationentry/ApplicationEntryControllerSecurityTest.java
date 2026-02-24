package uk.gov.hmcts.appregister.controller.applicationentry;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationEntryControllerSecurityTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-list-entries";
    private static final String CREATE_ENTRY_CONTEXT = "application-lists";

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
            RestEndpointDescription.builder()
                .url(getLocalUrl(WEB_CONTEXT))
                .method(HttpMethod.GET)
                .successRole(RoleEnum.USER)
                .successRole(RoleEnum.ADMIN)
                .build(),
            RestEndpointDescription.builder()
                .url(getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"))
                .method(HttpMethod.POST)
                .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                .successRole(RoleEnum.USER)
                .successRole(RoleEnum.ADMIN)
                .build(),
            RestEndpointDescription.builder()
                .url(
                    getLocalUrl(
                        CREATE_ENTRY_CONTEXT
                            + "/"
                            + UUID.randomUUID()
                            + "/entries/"
                            + UUID.randomUUID()))
                .method(HttpMethod.PUT)
                .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                .successRole(RoleEnum.USER)
                .successRole(RoleEnum.ADMIN)
                .build(),
            RestEndpointDescription.builder()
                .url(
                    getLocalUrl(
                        CREATE_ENTRY_CONTEXT
                            + "/"
                            + UUID.randomUUID()
                            + "/entries/"
                            + UUID.randomUUID()))
                .method(HttpMethod.GET)
                .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                .successRole(RoleEnum.USER)
                .successRole(RoleEnum.ADMIN)
                .build());
    }
}
