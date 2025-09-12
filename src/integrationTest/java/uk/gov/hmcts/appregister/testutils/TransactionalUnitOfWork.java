package uk.gov.hmcts.appregister.testutils;

import java.util.concurrent.Callable;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

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

    public void expectAccountable(Accountable expected, Accountable actual) {
        Assertions.assertEquals(expected.getCreatedUser(), actual.getCreatedUser());
    }

    public void expectVersionable(Versionable expected, Versionable actual) {
        Assertions.assertEquals(expected.getVersion(), actual.getVersion());
    }

    public void expectChangeable(Changeable expected, Changeable actual) {
        Assertions.assertEquals(expected.getChangedBy(), actual.getChangedBy());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(expected.getChangedDate(), actual.getChangedDate()));
    }

    public void expectAllCommonEntityFields(Object expected, Object actual) {
        if (expected instanceof Accountable expectedAccountable
                && actual instanceof Accountable actualAccountable) {
            expectAccountable(expectedAccountable, actualAccountable);
        }
        if (expected instanceof Versionable expectedVersionable
                && actual instanceof Versionable actualVersionable) {
            expectVersionable(expectedVersionable, actualVersionable);
        }
        if (expected instanceof Changeable expectedChangeable
                && actual instanceof Changeable actualChangeable) {
            expectChangeable(expectedChangeable, actualChangeable);
        }
    }
}
