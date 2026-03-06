package uk.gov.hmcts.appregister.controller.criminaljustice;

import java.time.OffsetDateTime;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class CriminalJusticeControllerSecurityTest extends AbstractSecurityControllerTest {
    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        AbstractCriminalJusticeControllerCrudTest.WEB_CONTEXT
                                                + "/"
                                                + AbstractCriminalJusticeControllerCrudTest
                                                        .EXPECTED_CODE,
                                        OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        AbstractCriminalJusticeControllerCrudTest.WEB_CONTEXT,
                                        OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
