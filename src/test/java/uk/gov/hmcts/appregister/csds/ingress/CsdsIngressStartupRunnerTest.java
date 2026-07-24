package uk.gov.hmcts.appregister.csds.ingress;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class CsdsIngressStartupRunnerTest {
    @Mock private CsdsIngressProcessor csdsIngressProcessor;
    @Mock private ConfigurableApplicationContext applicationContext;
    @InjectMocks private CsdsIngressStartupRunner runner;

    @Test
    void given_applicationStarts_when_run_then_executesIngress() {
        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(csdsIngressProcessor).runIngress();
        verify(applicationContext).close();
    }
}
