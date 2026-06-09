package uk.gov.hmcts.appregister.csds.ingress;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "appreg.csds.ingress", name = "enabled", havingValue = "true")
public class CsdsIngressProcessor {
    public static final String DATABASE_JOB_NAME = "CSDS_DATA_INGRESS";

    private final CsdsIngressProperties properties;
    private final CsdsIngressClient ingressClient;
    private final DistributedJobLockService distributedJobLockService;
    private final List<IDataIngressProcessor<?>> processors;

    public boolean runIngress() {
        return distributedJobLockService.executeWithLock(
                DATABASE_JOB_NAME, properties.getLeaseDuration(), this::runProcessors);
    }

    private void runProcessors() {
        if (processors.isEmpty()) {
            log.info("Skipping CSDS ingress because no data ingress processors are registered");
            return;
        }

        for (val processor : processors) {
            runProcessor(processor);
        }
    }

    private <T> void runProcessor(IDataIngressProcessor<T> processor) {
        log.info(
                "Starting CSDS ingress processor {} for target {}.{}",
                processor.datasetName(),
                processor.targetTable(),
                processor.targetKeyField());

        val rawJson = processor.retrieve(ingressClient);
        val processedData = processor.preProcess(rawJson);
        processor.handle(processedData);

        log.info(
                "Completed CSDS ingress processor {} for target {}.{}",
                processor.datasetName(),
                processor.targetTable(),
                processor.targetKeyField());
    }
}
