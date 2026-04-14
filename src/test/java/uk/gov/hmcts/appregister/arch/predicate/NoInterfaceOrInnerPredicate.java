package uk.gov.hmcts.appregister.arch.predicate;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;

public class NoInterfaceOrInnerPredicate extends DescribedPredicate<JavaMethod> {
    public NoInterfaceOrInnerPredicate() {
        super("have a preauth check ");
    }

    @Override
    public boolean test(JavaMethod javaClass) {
        return !javaClass.getOwner().isInterface() && !javaClass.getOwner().getName().contains("$");
    }
}
