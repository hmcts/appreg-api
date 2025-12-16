package uk.gov.hmcts.appregister.testutils;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A unit of work class that allows us to write custom code in a transactional.
 */
@Component
public class TransactionalUnitOfWork {
    @Transactional()
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @Transactional
    public <T> T inTransaction(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
