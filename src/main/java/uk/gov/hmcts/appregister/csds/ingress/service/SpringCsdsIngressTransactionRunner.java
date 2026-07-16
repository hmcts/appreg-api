package uk.gov.hmcts.appregister.csds.ingress.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringCsdsIngressTransactionRunner implements CsdsIngressTransactionRunner {
    private final TransactionTemplate transactionTemplate;

    public SpringCsdsIngressTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        return transactionTemplate.execute(unused -> supplier.get());
    }
}
