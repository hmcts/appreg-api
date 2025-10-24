package uk.gov.hmcts.appregister.testutils;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;
import uk.gov.hmcts.appregister.testutils.docker.PostgresCommand;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.TokenStub;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@Slf4j
public class BaseIntegration extends BasePostgresIntegrationTest {

    @Autowired protected TokenStub tokenStub;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired protected RestAssuredClient restAssuredClient;

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    protected String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences[0]}")
    protected String audience;

    @Autowired protected WireMockServer wireMockServer;

    protected static PostgresCommand postgresCommand = new PostgresCommand();

    protected LogCaptor logCaptor;

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
        logCaptor.clearLogs();
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
}
