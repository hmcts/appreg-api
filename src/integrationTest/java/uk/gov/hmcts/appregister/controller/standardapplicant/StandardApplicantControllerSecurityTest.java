package uk.gov.hmcts.appregister.controller.standardapplicant;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class StandardApplicantControllerSecurityTest extends AbstractSecurityControllerTest {
    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        AbstractStandardApplicantControllerCrudTest.WEB_CONTEXT
                                                + "/"
                                                + AbstractStandardApplicantControllerCrudTest
                                                        .APPCODE_CODE
                                                + "?date="
                                                + LocalDate.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(AbstractStandardApplicantControllerCrudTest.WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
