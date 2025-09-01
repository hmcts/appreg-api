package uk.gov.hmcts.appregister.testutils.docker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;


@Slf4j
public class PostgresCommand implements Command {

    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";
    private final static String DATABASE_NAME = "db";

    private final PostgreSQLContainer<?> container
        = new PostgreSQLContainer("postgres:17-alpine");

    {
        container.withPassword(USERNAME);
        container.withDatabaseName(DATABASE_NAME);
        container.withUsername(PASSWORD);
        container.withCopyToContainer(
            MountableFile.forHostPath("./init/001_init.sql", 777),
            "/docker-entrypoint-initdb.d/init.sql"
        );
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
            container.withCopyToContainer(
                MountableFile.forHostPath("./init/001_init.sql", 777),
                "/docker-entrypoint-initdb.d/init.sql"
            );

            container.start();
        }

        registry.add("spring.datasource.url", () -> container.getJdbcUrl());
        registry.add("spring.datasource.username", () -> container.getUsername());
        registry.add("spring.datasource.password", () -> container.getPassword());

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
}
