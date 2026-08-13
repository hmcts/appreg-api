package uk.gov.hmcts.appregister.controller.job;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.audit.JobAuditOperation;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

@Import(AuditConnectionPoolStarvationIntegrationTest.BarrierConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.datasource.hikari.maximum-pool-size=2",
            "spring.datasource.hikari.minimum-idle=0",
            "spring.datasource.hikari.connection-timeout=500"
        })
class AuditConnectionPoolStarvationIntegrationTest extends BaseIntegration {
    private static final int CONCURRENT_REQUESTS = 2;

    @Autowired private AsyncJobRepository asyncJobRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Test
    void givenPoolIsFullyUsed_whenAuditedRequestsRunConcurrently_thenRequestsAndAuditsComplete()
            throws Exception {
        val tokenGenerator = getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(tokenGenerator.getJwtFromToken()));
        val job =
                asyncJobRepository.saveAndFlush(
                        AsyncJob.builder()
                                .jobState(JobStatusType.COMPLETED)
                                .jobType(JobType.FEES_REPORT.getValue())
                                .build());
        val jobUrl = getLocalUrl("jobs/" + job.getUuid());
        val token = tokenGenerator.fetchTokenForRole();

        dataAuditRepository.deleteAll();

        try (val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            val requests =
                    List.of(
                            executor.submit(
                                    () -> restAssuredClient.executeGetRequest(jobUrl, token)),
                            executor.submit(
                                    () -> restAssuredClient.executeGetRequest(jobUrl, token)));

            val responses =
                    requests.stream()
                            .map(
                                    request -> {
                                        try {
                                            return request.get(5, SECONDS);
                                        } catch (Exception exception) {
                                            throw new AssertionError(
                                                    "Concurrent audited request did not complete",
                                                    exception);
                                        }
                                    })
                            .toList();

            responses.forEach(response -> response.then().statusCode(200));
        }

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                Assertions.assertEquals(
                                        CONCURRENT_REQUESTS,
                                        countJobIdAudits(job),
                                        "Every successful request should persist its audit row"));
    }

    private long countJobIdAudits(AsyncJob job) {
        return dataAuditRepository.findAll().stream()
                .filter(row -> TableNames.ASYNC_JOBS.equals(row.getTableName()))
                .filter(row -> "id".equals(row.getColumnName()))
                .filter(row -> job.getUuid().toString().equals(row.getNewValue()))
                .count();
    }

    @TestConfiguration
    static class BarrierConfiguration {
        @Bean
        AuditOperationLifecycleListener auditConnectionBarrier() {
            return new AuditConnectionBarrier(CONCURRENT_REQUESTS);
        }
    }

    static class AuditConnectionBarrier implements AuditOperationLifecycleListener, Ordered {
        private final CyclicBarrier barrier;

        AuditConnectionBarrier(int parties) {
            barrier = new CyclicBarrier(parties);
        }

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (!(event instanceof CompleteEvent)
                    || event.getRequestAction() != JobAuditOperation.GET_JOB_STATUS_AUDIT_EVENT) {
                return;
            }

            try {
                barrier.await(5, SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while waiting at the audit connection barrier", exception);
            } catch (BrokenBarrierException | TimeoutException exception) {
                throw new IllegalStateException(
                        "Concurrent requests did not reach the audit connection barrier",
                        exception);
            }
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
