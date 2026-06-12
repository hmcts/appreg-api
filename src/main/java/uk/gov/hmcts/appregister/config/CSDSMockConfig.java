package uk.gov.hmcts.appregister.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
@Profile("csds-mock")
public class CSDSMockConfig {

    @Value("${appreg.csds.wiremock.port:0}")
    private int wireMockPort;

    private final String STUBS_PATH = "csds/stubs";

    // This class is used to activate the csds-wiremock profile which will start a WireMock server
    // with the CSDS stubs
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer csdsWireMockServer() {
        log.warn("Starting WireMock server for CSDS stubs on port {}", wireMockPort);

        WireMockConfiguration config = new WireMockConfiguration();
        config.port(wireMockPort);
        config.fileSource(fileSource());
        return new WireMockServer(config);
    }

    private FileSource fileSource() {
        FileSource fs;
        try {
            fs = new ClasspathFileSource(STUBS_PATH);
        } catch (Exception x) {
            log.debug("Running via executable jar; using BOOT-INF folder.");
            fs = new ClasspathFileSource("BOOT-INF/classes/" + STUBS_PATH);
        }
        return fs;
    }
}
