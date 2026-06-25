package uk.gov.hmcts.appregister.controller.standardapplicant;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

class AbstractStandardApplicantControllerCrudTest extends BaseIntegration {

    protected static final String WEB_CONTEXT = "standard-applicants";

    @MockitoBean protected Clock clock; // replaces Clock bean in Spring context

    protected static final String APPCODE_CODE = "APP001";
}
