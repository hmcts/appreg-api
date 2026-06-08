package uk.gov.hmcts.appregister.csds.ingress;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.enabled=true",
            "appreg.csds.ingress.processors.dummy-logging.enabled=true",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}",
            "appreg.csds.ingress.access-keys[0]=primary-test-key",
            "appreg.csds.ingress.access-keys[1]=secondary-test-key"
        })
class CsdsIngressProcessorIntegrationTest extends BaseRepositoryTest {
    @Autowired private CsdsIngressProcessor csdsIngressProcessor;
    @Autowired private DatabaseJobRepository databaseJobRepository;

    private LogCaptor dummyProcessorLogs;

    @BeforeEach
    void setUpJobRow() {
        var databaseJob = databaseJobRepository.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);

        if (databaseJob == null) {
            databaseJob =
                    databaseJobRepository.save(
                            DatabaseJob.builder()
                                    .name(CsdsIngressProcessor.DATABASE_JOB_NAME)
                                    .enabled(YesOrNo.YES)
                                    .build());
        }

        databaseJob.setEnabled(YesOrNo.YES);
        databaseJob.setMetadata(null);
        databaseJob.setLastRan(null);
        databaseJobRepository.save(databaseJob);

        dummyProcessorLogs = LogCaptor.forClass(DummyLoggingDataIngressProcessor.class);
        dummyProcessorLogs.clearLogs();
    }

    @Test
    void given_dummyProcessorWired_when_runIngress_then_executesUnderDistributedLock() {
        stubFor(
                get(urlPathEqualTo("/dummy/primary"))
                        .withHeader("x-api-key", equalTo("primary-test-key"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"endpoint\":\"primary\"}")));
        stubFor(
                get(urlPathEqualTo("/dummy/secondary"))
                        .withHeader("x-api-key", equalTo("primary-test-key"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"endpoint\":\"secondary\"}")));

        var executed = csdsIngressProcessor.runIngress();
        var databaseJob = databaseJobRepository.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);

        assertThat(executed).isTrue();
        assertThat(databaseJob.getMetadata()).isNull();
        assertThat(databaseJob.getLastRan()).isNull();
        assertThat(dummyProcessorLogs.getInfoLogs())
                .anyMatch(log -> log.contains("retrieve invoked"))
                .anyMatch(log -> log.contains("preProcess invoked"))
                .anyMatch(log -> log.contains("handle invoked"));

        verify(
                getRequestedFor(urlPathEqualTo("/dummy/primary"))
                        .withHeader("x-api-key", equalTo("primary-test-key")));
        verify(
                getRequestedFor(urlPathEqualTo("/dummy/secondary"))
                        .withHeader("x-api-key", equalTo("primary-test-key")));
    }
}
