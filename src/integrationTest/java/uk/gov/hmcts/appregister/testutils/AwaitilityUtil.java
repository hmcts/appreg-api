package uk.gov.hmcts.appregister.testutils;

import static org.awaitility.Awaitility.await;

import io.restassured.response.Response;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

/**
 * A utility class that uses the awaitility API to poll for a condition.
 */
public final class AwaitilityUtil {

    private AwaitilityUtil() {
        // no-op
    }

    public static void waitForMax10SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        waitForMaxWithOneSecondPoll(callable, Duration.ofSeconds(10));
    }

    public static void waitForMax10SecondsWithOneSecondPoll(Runnable runnable) {
        waitForMaxWithOneSecondPoll(runnable, Duration.ofSeconds(10));
    }

    public static JobAcknowledgement waitForJobToReachTerminalStatus(
            RestAssuredClient restAssuredClient, URL jobStatusUrl, TokenAndJwksKey token) {
        return waitForJobToReachTerminalStatus(
                restAssuredClient, jobStatusUrl, token, Duration.ofSeconds(30));
    }

    public static JobAcknowledgement waitForJobToReachTerminalStatus(
            RestAssuredClient restAssuredClient,
            URL jobStatusUrl,
            TokenAndJwksKey token,
            Duration duration) {
        AtomicReference<JobAcknowledgement> jobStatus = new AtomicReference<>();

        waitForMaxWithOneSecondPoll(
                () -> {
                    Response response = restAssuredClient.executeGetRequest(jobStatusUrl, token);

                    if (response.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);
                    jobStatus.set(acknowledgement);

                    return isTerminalJobStatus(acknowledgement.getStatus());
                },
                duration);

        return Optional.ofNullable(jobStatus.get())
                .orElseThrow(
                        () ->
                                new AssertionFailure(
                                        "Job status not available within the specified time"));
    }

    public static boolean waitForMaxWithOneSecondPoll(
            Callable<Boolean> callable, Duration duration) {
        WaitingResult waitingResult = new WaitingResult(callable);

        var awaitAlias = RandomStringUtils.insecure().nextAlphanumeric(4);
        await(awaitAlias)
                .atMost(duration)
                .with()
                .pollInterval(Duration.ofSeconds(1))
                .until(waitingResult);

        if (!waitingResult.isCriteriaMet()) {
            throw new AssertionFailure("Criteria not met within the specified time");
        }
        return waitingResult.isCriteriaMet();
    }

    public static void waitForMaxWithOneSecondPoll(Runnable runnable, Duration duration) {
        var awaitAlias = RandomStringUtils.insecure().nextAlphanumeric(4);
        await(awaitAlias)
                .atMost(duration)
                .with()
                .pollInterval(Duration.ofSeconds(1))
                .until(
                        () -> {
                            try {
                                runnable.run();
                            } catch (Exception | Error e) {
                                // ignore the error for now
                                return false;
                            }
                            return true;
                        });
    }

    private static boolean isTerminalJobStatus(JobStatus1 status) {
        return status == JobStatus1.COMPLETED || status == JobStatus1.FAILED;
    }

    @RequiredArgsConstructor
    @Getter
    static class WaitingResult implements Callable<Boolean> {
        public final Callable<Boolean> criteria;

        public boolean criteriaMet;

        @Override
        public Boolean call() throws Exception {
            criteriaMet = criteria.call();
            return criteriaMet;
        }
    }
}
