package uk.gov.hmcts.appregister.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.DataAuditPersistenceQueue;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
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
    private static final int JWKS_STUB_MAX_ATTEMPTS = 5;
    private static final long JWKS_STUB_RETRY_DELAY_MILLIS = 200L;

    @Autowired protected TokenStub tokenStub;

    @Autowired protected RestAssuredClient restAssuredClient;

    @Autowired private DataAuditPersistenceQueue dataAuditPersistenceQueue;

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
        stubExternalJwksKeysWithRetry();

        logCaptor = LogCaptor.forClass(AuditOperationSlf4jLogger.class);
        activityAuditLogAsserter = new ActivityAuditLogAsserter();
        logCaptor.clearLogs();
        differenceLogAsserter.clearLogs();
        mapper = SHARED_OBJECT_MAPPER;
    }

    private void stubExternalJwksKeysWithRetry() {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= JWKS_STUB_MAX_ATTEMPTS; attempt++) {
            try {
                // Populate the JWKS endpoint with a global public key for resource-server auth.
                tokenStub.stubExternalJwksKeysOnce(GLOBAL_JWKS_KEY);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn(
                        "WireMock JWKS stub setup failed on attempt {}/{}",
                        attempt,
                        JWKS_STUB_MAX_ATTEMPTS,
                        exception);

                if (attempt < JWKS_STUB_MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(JWKS_STUB_RETRY_DELAY_MILLIS);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Interrupted while retrying WireMock JWKS stub setup",
                                interruptedException);
                    }
                }
            }
        }

        throw new IllegalStateException("Error setting up WireMock JWKS stub", lastFailure);
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

    protected void clearDataAudits(DataAuditRepository dataAuditRepository) {
        awaitDataAudits();
        dataAuditRepository.deleteAll();
    }

    protected void awaitDataAudits() {
        dataAuditPersistenceQueue.awaitIdle(Duration.ofSeconds(5));
    }
}
