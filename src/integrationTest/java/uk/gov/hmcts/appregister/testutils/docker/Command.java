package uk.gov.hmcts.appregister.testutils.docker;

import java.io.IOException;
import org.springframework.test.context.DynamicPropertyRegistry;

/** A command that allows us to start and stop a docker container. */
public interface Command {

    /** Stops the container and cleans up any resources. */
    void cleanupResources();

    /**
     * Starts the container and registers any dynamic properties.
     *
     * @param dynamicPropertyRegistry Sets up the Spring environment with the container properties
     */
    void start(DynamicPropertyRegistry dynamicPropertyRegistry) throws IOException;

    /** Returns true if the container started successfully. */
    boolean isSuccess();

    /** Returns the port that the container is mapped to on the host machine. */
    Integer getPortForContainer();

    /** Returns true if the container is currently running. */
    boolean isRunning();
}
