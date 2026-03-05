package uk.gov.hmcts.appregister.controller.applicationcode;

import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class AbstractApplicationCodeEntryCrudTest extends BaseIntegration {
    protected static final String WEB_CONTEXT = "application-codes";

    @Value("${spring.sql.init.schema-locations}")
    protected String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    protected Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    protected Integer maxPageSize;

    @MockitoBean protected Clock clock; // replaces Clock bean in Spring context

    // The total app codes inserted by flyway scripts. See V6__InitialTestData.sql
    protected static final int TOTAL_APP_CODES_COUNT = 45;

    protected static final String FEE_DESCRIPTION = "JP perform function away from court";
    protected static final String OFFSITE_FEE_DESCRIPTION =
            "Offsite: JP perform function away from court";
    protected static final String APPCODE_CODE = "AD99002";
    protected static final String DUPLICATE_APPCODE_CODE = "MS99006";

    protected static final String DATE_TO_FIND_CODE = "2016-01-01T00:00Z";
    protected static final String START_AUDIT_LOG = "Start audit";
    protected static final String COMPLETION_AUDIT_LOG = "Completion audit";

    protected static final String GET_APPCODE_AUDIT_ACTION = "Get Application Code";
    protected static final String GET_APPCODES_AUDIT_ACTION = "Get Application Codes";
    protected static final String CURRENT_TIME = "2020-07-25T00:00:00Z";

    /** Build a token generator with ADMIN role. */
    protected TokenGenerator createAdminToken() {
        return getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
    }

    @BeforeEach
    public void before() {
        // a date that is without range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
    }
}
