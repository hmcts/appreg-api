package uk.gov.hmcts.appregister.testutils.docker;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.util.Map;

public interface Command {
    void cleanupResources();

    void start(DynamicPropertyRegistry dynamicPropertyRegistry) throws IOException;

    boolean isSuccess();

    Integer getPortForContainer();

    boolean isRunning();
}
