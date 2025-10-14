package uk.gov.hmcts.appregister.testutils.docker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

/**
 * A class that isolates the test container configuration around launching the postgres container.
 * The class understands how to apply itself to the Spring configuration for the application
 * registration service
 */
@Slf4j
public class PostgresCommand implements Command {

    private static final String USERNAME = "app_reg_user";
    private static final String PASSWORD = "password";
    private static final String DATABASE_NAME = "appreg-db";

    private final PostgreSQLContainer<?> container =
            new PostgreSQLContainer<>("postgres:17-alpine");

    {
        container
                .withPassword(PASSWORD)
                .withDatabaseName(DATABASE_NAME)
                .withUsername(USERNAME)
                .withCopyToContainer(
                        MountableFile.forHostPath("./init/001_init.sql"),
                        "/docker-entrypoint-initdb.d/init.sql");
    }

    @Override
    public void cleanupResources() {
        container.stop();
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public void start(DynamicPropertyRegistry registry) {
        if (!container.isRunning()) {
            container.withPassword(USERNAME);
            container.withDatabaseName(DATABASE_NAME);
            container.withUsername(PASSWORD);

            container.start();
        }

        if (registry != null) {
            registry.add("spring.datasource.url", () -> container.getJdbcUrl());
            registry.add("spring.datasource.username", () -> container.getUsername());
            registry.add("spring.datasource.password", () -> container.getPassword());
        }
        container.withLogConsumer(new Slf4jLogConsumer(log));
    }

    @Override
    public Integer getPortForContainer() {
        return container.getMappedPort(5432);
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }

    public void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> container.getJdbcUrl());
        registry.add("spring.datasource.username", () -> container.getUsername());
        registry.add("spring.datasource.password", () -> container.getPassword());
    }

    public static void main(String[] args) throws Exception {
        PostgresCommand postgresCommand = new PostgresCommand();
        postgresCommand.start(null);
        System.out.println("Postgres started on port: " + postgresCommand.getPortForContainer());
        System.in.read();
    }
}
