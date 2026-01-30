package uk.gov.hmcts.appregister.testutils;

import com.nimbusds.jose.JOSEException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;

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

    protected AuditLogAsserter differenceLogAsserter;

    @Value("${wiremock.server.port}")
    protected String token;

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
        differenceLogAsserter = new AuditLogAsserter();
        logCaptor.clearLogs();
        differenceLogAsserter.clearLogs();
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
}
