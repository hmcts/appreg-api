package uk.gov.hmcts.appregister.controller.admin;

import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class AdminControllerSecurityTest extends AbstractSecurityControllerTest {
    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        AbstractAdminAPICrudTest.WEB_CONTEXT
                                                + "/"
                                                + AdminJobType.APPLICATION_LISTS_DATABASE_JOB
                                                        .name()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.ADMIN)
                        .invalidRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        AbstractAdminAPICrudTest.WEB_CONTEXT
                                                + "/"
                                                + AdminJobType.APPLICATION_LISTS_DATABASE_JOB.name()
                                                + "?enable=true"))
                        .method(HttpMethod.PUT)
                        .successRole(RoleEnum.ADMIN)
                        .invalidRole(RoleEnum.USER)
                        .build());
    }
}
