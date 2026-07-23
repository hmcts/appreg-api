package uk.gov.hmcts.appregister.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngestService;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.TokenStub;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ActivityAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;

@Slf4j
public class BaseIntegration extends BasePostgresIntegrationTest {

    private static final ObjectMapper SHARED_OBJECT_MAPPER = createObjectMapper();
    private static final String GLOBAL_JWKS_KEY = TokenGenerator.builder().build().getGlobalKey();

    @Autowired protected TokenStub tokenStub;

    @Autowired protected RestAssuredClient restAssuredClient;

    @MockitoBean protected CsdsIngestService csdsIngestService;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    protected String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences[0]}")
    protected String audience;

    protected LogCaptor logCaptor;

    /** A data audit log asserter. */
    @Autowired protected DataAuditLogAsserter differenceLogAsserter;

    /** An activity log asserter. */
    protected ActivityAuditLogAsserter activityAuditLogAsserter;

    /** A mapper that can be used to convert objects to json strings. */
    protected ObjectMapper mapper;

    @BeforeEach
    void setup() {
        try {
            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeysOnce(GLOBAL_JWKS_KEY);
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }

        logCaptor = LogCaptor.forClass(AuditOperationSlf4jLogger.class);
        activityAuditLogAsserter = new ActivityAuditLogAsserter();
        logCaptor.clearLogs();
        differenceLogAsserter.clearLogs();
        mapper = SHARED_OBJECT_MAPPER;
    }

    private static ObjectMapper createObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new JsonNullableModule());
        return objectMapper;
    }

    /**
     * gets a token that has the correct audience and issuer.
     *
     * @return The token builder
     */
    public TokenGenerator.TokenGeneratorBuilder getATokenWithValidCredentials() {
        return TokenGenerator.builder().issuer(issuer).audience(audience);
    }

    public TokenAndJwksKey getToken() throws JOSEException {
        return getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();
    }
}
