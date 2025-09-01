package uk.gov.hmcts.appregister;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.hmcts.appregister.testutils.docker.PostgresCommand;
import uk.gov.hmcts.appregister.testutils.stubs.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.stubs.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.stubs.AppRegistryDatabaseStub;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.TokenStub;

@ActiveProfiles({"int"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@Slf4j
public class BaseIntegration {

    protected TokenStub tokenStub = new TokenStub();

    @Autowired
    protected WebTestClient webTestClient;

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    @Value("$(server.port)")
    protected String port;

    @Autowired
    protected AppRegistryDatabaseStub appRegistryDatabaseStub;

    @Autowired
    protected WireMockServer wireMockServer;

    protected static PostgresCommand postgresCommand = new PostgresCommand();

    @BeforeEach
    void setup() {
        try {
            log.info("Wiremock Port: " + wiremockPort);

            // clear any data that we have added to the database
            appRegistryDatabaseStub.clearDb();

            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(TokenGenerator.builder().build().getGlobalKey());
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
            postgresCommand.start(registry);
    }

    public WebTestClient.RequestHeadersSpec<?> createRequest(String context, TokenAndJwksKey token) {
        return webTestClient.get().uri("http://localhost:" + port + "/" + context)
            .header("Authorization", "Bearer " + token);
    }
}
