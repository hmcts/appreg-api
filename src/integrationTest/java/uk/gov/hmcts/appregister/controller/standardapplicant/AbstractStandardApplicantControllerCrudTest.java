package uk.gov.hmcts.appregister.controller.standardapplicant;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

public class AbstractStandardApplicantControllerCrudTest extends BaseIntegration {

    protected static final String WEB_CONTEXT = "standard-applicants";

    @Value("${spring.data.web.pageable.default-page-size}")
    protected Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    protected Integer maxPageSize;

    @MockitoBean protected Clock clock; // replaces Clock bean in Spring context

    // The total standard applicant inserted by flyway scripts. See V6__InitialTestData.sql
    protected static final int TOTAL_STANDARD_APPLICANT_COUNT = 7;

    protected static final String APPCODE_CODE = "APP001";
    protected static final String APPCODE_CODE_ORGANISATION = "APP005";

    protected static final String DUPLICATE_APPCODE_CODE = "APP003";
}
