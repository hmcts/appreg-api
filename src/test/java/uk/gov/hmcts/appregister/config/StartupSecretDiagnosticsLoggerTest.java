package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class StartupSecretDiagnosticsLoggerTest {
    @TempDir Path tempDir;

    @Test
    void givenMountedSecretsAndResolvedValues_whenRun_thenLogsMountedAndResolvedState()
            throws Exception {
        Files.writeString(
                tempDir.resolve(StartupSecretDiagnosticsLogger.POSTGRES_DATABASE), "appreg-db");
        Files.writeString(
                tempDir.resolve(StartupSecretDiagnosticsLogger.CSDS_BASE_URL),
                "https://csds.dev.apps.hmcts.net/api/rest");

        var environment =
                new MockEnvironment()
                        .withProperty(StartupSecretDiagnosticsLogger.POSTGRES_DATABASE, "appreg-db")
                        .withProperty(
                                StartupSecretDiagnosticsLogger.CSDS_BASE_URL,
                                "https://csds.dev.apps.hmcts.net/api/rest");
        var logger = new StartupSecretDiagnosticsLogger(environment, tempDir.toString());
        var logCaptor = LogCaptor.forClass(StartupSecretDiagnosticsLogger.class);
        logCaptor.clearLogs();

        logger.run(null);

        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "Startup secret diagnostic: property=POSTGRES_DATABASE resolvedValue=appreg-db "
                                + "usingDefault=true mountedFilePresent=true mountedFileReadable=true",
                        "Startup secret diagnostic: property=CSDS_BASE_URL "
                                + "resolvedValue=https://csds.dev.apps.hmcts.net/api/rest "
                                + "usingDefault=false mountedFilePresent=true mountedFileReadable=true");
    }

    @Test
    void givenMissingCsdsFileAndFallbackValue_whenRun_thenLogsDefaultAndMissingFile()
            throws Exception {
        Files.writeString(
                tempDir.resolve(StartupSecretDiagnosticsLogger.POSTGRES_DATABASE), "appreg-db");

        var environment = new MockEnvironment();
        var logger = new StartupSecretDiagnosticsLogger(environment, tempDir.toString());
        var logCaptor = LogCaptor.forClass(StartupSecretDiagnosticsLogger.class);
        logCaptor.clearLogs();

        logger.run(null);

        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "Startup secret diagnostic: property=CSDS_BASE_URL resolvedValue=http://noop "
                                + "usingDefault=true mountedFilePresent=false mountedFileReadable=false");
    }
}
