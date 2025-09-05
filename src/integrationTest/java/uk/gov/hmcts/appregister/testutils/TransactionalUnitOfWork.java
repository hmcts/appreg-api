package uk.gov.hmcts.appregister.testutils;

import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** A unit of work class that allows us to write custom code in a transactional. */
@Component
public class TransactionalUnitOfWork {
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Transactional()
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @Transactional
    public <T> T inTransaction(Callable<T> supplier) {
        try {
            return supplier.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
