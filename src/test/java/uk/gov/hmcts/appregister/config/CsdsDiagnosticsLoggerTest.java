package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class CsdsDiagnosticsLoggerTest {
    @TempDir Path tempDir;

    @Test
    void givenMountedSecretsAndResolvedValues_whenRun_thenLogsMountedAndResolvedState()
            throws IOException {
        Files.writeString(
                tempDir.resolve(CsdsDiagnosticsLogger.CSDS_BASE_URL),
                "https://csds.dev.apps.hmcts.net/api/rest");
        Files.writeString(tempDir.resolve(CsdsDiagnosticsLogger.CSDS_KEY_1), "secret-1");
        Files.writeString(tempDir.resolve(CsdsDiagnosticsLogger.CSDS_KEY_2), "secret-2");
        Files.writeString(tempDir.resolve(CsdsDiagnosticsLogger.CSDS_SCHEDULE_HOUR), "3");
        Files.writeString(tempDir.resolve(CsdsDiagnosticsLogger.CSDS_SCHEDULE_MINUTE), "0");

        var environment =
                new MockEnvironment()
                        .withProperty(
                                CsdsDiagnosticsLogger.CSDS_BASE_URL,
                                "https://csds.dev.apps.hmcts.net/api/rest")
                        .withProperty(CsdsDiagnosticsLogger.CSDS_KEY_1, "secret-1")
                        .withProperty(CsdsDiagnosticsLogger.CSDS_KEY_2, "secret-2")
                        .withProperty(CsdsDiagnosticsLogger.CSDS_SCHEDULE_HOUR, "19")
                        .withProperty(CsdsDiagnosticsLogger.CSDS_SCHEDULE_MINUTE, "0")
                        .withProperty("appreg.csds.ingress.schedule.enabled", "false")
                        .withProperty("appreg.csds.ingress.schedule.poll-interval", "PT10M")
                        .withProperty("appreg.csds.ingress.lease-duration", "PT10M")
                        .withProperty("appreg.csds.ingress.page-size", "200")
                        .withProperty(
                                "appreg.csds.ingress.processors.application-codes.enabled", "true")
                        .withProperty(
                                "appreg.csds.ingress.processors.resolution-codes.enabled", "false")
                        .withProperty("appreg.csds.ingress.processors.fee.enabled", "true")
                        .withProperty(
                                "appreg.csds.ingress.processors.standard-applicants.enabled",
                                "false")
                        .withProperty(
                                "appreg.csds.ingress.processors.national-court-houses.enabled",
                                "true");
        var logger = new CsdsDiagnosticsLogger(environment, tempDir.toString());
        var logCaptor = LogCaptor.forClass(CsdsDiagnosticsLogger.class);
        logCaptor.clearLogs();

        logger.run(null);

        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "CSDS secret diagnostic: property=CSDS_BASE_URL "
                                + "resolvedValue=https://csds.dev.apps.hmcts.net/api/rest "
                                + "usingDefault=false mountedFilePresent=true mountedFileReadable=true",
                        "CSDS secret diagnostic: property=CSDS_KEY_1 resolvedValue=se***-1 "
                                + "usingDefault=false mountedFilePresent=true mountedFileReadable=true",
                        "CSDS secret diagnostic: property=CSDS_KEY_2 resolvedValue=se***-2 "
                                + "usingDefault=false mountedFilePresent=true mountedFileReadable=true",
                        "CSDS secret diagnostic: property=CSDS_SCHEDULE_HOUR "
                                + "resolvedValue=19 usingDefault=false mountedFilePresent=true "
                                + "mountedFileReadable=true",
                        "CSDS secret diagnostic: property=CSDS_SCHEDULE_MINUTE "
                                + "resolvedValue=0 usingDefault=true mountedFilePresent=true "
                                + "mountedFileReadable=true",
                        "CSDS config diagnostic: scheduleEnabled=false scheduleHour=19 "
                                + "scheduleMinute=0 schedulePollInterval=PT10M leaseDuration=PT10M pageSize=200 "
                                + "processors=application-codes:true resolution-codes:false fee:true "
                                + "standard-applicants:false national-court-houses:true");
    }

    @Test
    void givenMissingCsdsFileAndFallbackValue_whenRun_thenLogsDefaultAndMissingFile() {
        var environment = new MockEnvironment();
        var logger = new CsdsDiagnosticsLogger(environment, tempDir.toString());
        var logCaptor = LogCaptor.forClass(CsdsDiagnosticsLogger.class);
        logCaptor.clearLogs();

        logger.run(null);

        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "CSDS secret diagnostic: property=CSDS_BASE_URL resolvedValue=http://noop "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false",
                        "CSDS secret diagnostic: property=CSDS_KEY_1 resolvedValue=mi***_1 "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false",
                        "CSDS secret diagnostic: property=CSDS_KEY_2 resolvedValue=mi***_2 "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false",
                        "CSDS secret diagnostic: property=CSDS_SCHEDULE_HOUR resolvedValue=3 "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false",
                        "CSDS secret diagnostic: property=CSDS_SCHEDULE_MINUTE resolvedValue=0 "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false",
                        "CSDS config diagnostic: scheduleEnabled=true scheduleHour=3 scheduleMinute=0 "
                                + "schedulePollInterval=PT10M leaseDuration=PT5M pageSize=100 "
                                + "processors=application-codes:true "
                                + "resolution-codes:true fee:true standard-applicants:true "
                                + "national-court-houses:true");
    }
}
