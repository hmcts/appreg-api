package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;

public class MethodOwnerNoInterfaceOrInnerPredicate extends DescribedPredicate<JavaMethod> {
    public MethodOwnerNoInterfaceOrInnerPredicate() {
        super("method owner no interface or inner class");
    }

    @Override
    public boolean test(JavaMethod javaClass) {
        return !javaClass.getOwner().isInterface() && !javaClass.getOwner().getName().contains("$");
    }
}
