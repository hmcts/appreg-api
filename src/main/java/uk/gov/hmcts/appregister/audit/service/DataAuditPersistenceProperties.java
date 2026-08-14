package uk.gov.hmcts.appregister.audit.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "appreg.audit.persistence")
public class DataAuditPersistenceProperties {
    /** Maximum concurrent audit transactions and therefore audit database connections. */
    @Min(1)
    private int workerCount = 5;

    /** Maximum number of audit batches waiting behind active workers. */
    @Min(1)
    private int queueCapacity = 2000;

    /** Time allowed for queued audit work to drain during graceful application shutdown. */
    @NotNull private Duration shutdownTimeout = Duration.ofSeconds(30);

    @AssertTrue(message = "Audit persistence shutdown timeout must be positive")
    public boolean isShutdownTimeoutValid() {
        return shutdownTimeout != null
                && !shutdownTimeout.isNegative()
                && !shutdownTimeout.isZero();
    }
}
