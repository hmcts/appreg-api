package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.instancio.Select.field;

import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class ApplicationListControllerSecurityTest extends AbstractSecurityControllerTest {

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationListUpdateDto uploadPayload =
                Instancio.of(ApplicationListUpdateDto.class)
                        .withSettings(settings)
                        .ignore(field(ApplicationListUpdateDto::getCourtLocationCode))

                        // Instancio does not honour Max and Min annotations
                        .ignore(field(ApplicationListUpdateDto::getDurationHours))
                        .ignore(field(ApplicationListUpdateDto::getDurationMinutes))
                        .create();

        uploadPayload.setCjaCode(null);
        uploadPayload.setDurationHours(1);
        uploadPayload.setDurationMinutes(1);

        var validPayload =
                new ApplicationListCreateDto()
                        .date(AbstractApplicationListTest.TEST_DATE)
                        .time(AbstractApplicationListTest.TEST_TIME)
                        .description("sec-matrix")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(AbstractApplicationListTest.VALID_COURT_CODE);

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(AbstractApplicationListTest.WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(AbstractApplicationListTest.WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        AbstractApplicationListTest.WEB_CONTEXT
                                                + "/"
                                                + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        AbstractApplicationListTest.WEB_CONTEXT
                                                + "/"
                                                + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
