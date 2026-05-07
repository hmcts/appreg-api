package uk.gov.hmcts.appregister.controller.applicationcode;

import java.time.OffsetDateTime;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class ApplicationCodeControllerSecurityTest extends AbstractSecurityControllerTest {
    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(AbstractApplicationCodeEntryCrudTest.WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrlWithDate("application-codes/2", OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
