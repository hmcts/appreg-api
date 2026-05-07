package uk.gov.hmcts.appregister.controller.courtlocation;

import java.time.OffsetDateTime;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class CourtLocationControllerSecurityTest extends AbstractSecurityControllerTest {

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        AbstractCourtLocationControllerCrudTest.WEB_CONTEXT
                                                + "/"
                                                + AbstractCourtLocationControllerCrudTest
                                                        .CARDIFF_CODE,
                                        OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(AbstractCourtLocationControllerCrudTest.WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
