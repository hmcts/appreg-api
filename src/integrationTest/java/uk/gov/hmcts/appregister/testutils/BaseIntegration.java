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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;
import uk.gov.hmcts.appregister.testutils.docker.PostgresCommand;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.TokenStub;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ActivityAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;

@Slf4j
public class BaseIntegration extends BasePostgresIntegrationTest {

    @Autowired protected TokenStub tokenStub;

    @Autowired protected RestAssuredClient restAssuredClient;

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    protected String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences[0]}")
    protected String audience;

    protected static PostgresCommand postgresCommand = new PostgresCommand();

    protected LogCaptor logCaptor;

    /** A data audit log asserter. */
    protected DataAuditLogAsserter differenceLogAsserter;

    /** An activity log asserter. */
    protected ActivityAuditLogAsserter activityAuditLogAsserter;

    @Value("${wiremock.server.port}")
    protected String token;

    /** A mapper that can be used to convert objects to json strings. */
    protected ObjectMapper mapper;

    @BeforeEach
    void setup() {
        try {
            log.info("Wiremock Port: " + wiremockPort);

            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(TokenGenerator.builder().build().getGlobalKey());
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }

        logCaptor = LogCaptor.forClass(AuditOperationSlf4jLogger.class);
        differenceLogAsserter = new DataAuditLogAsserter();
        activityAuditLogAsserter = new ActivityAuditLogAsserter();
        logCaptor.clearLogs();
        differenceLogAsserter.clearLogs();
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JsonNullableModule());
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        postgresCommand.start(registry);
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

    protected String escapeRegex(String input) {
        return input.replaceAll(
            "[\\\\.^$|?*+()\\[\\]{}]",
            "\\\\$0"
        );
    }
}
