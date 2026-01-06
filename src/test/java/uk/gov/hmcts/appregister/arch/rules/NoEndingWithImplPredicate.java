package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * A predicate to filter based on a class not ending with Impl.
 */
public class NoEndingWithImplPredicate extends DescribedPredicate<JavaClass> {
    public NoEndingWithImplPredicate() {
        super("no end with impl");
    }

    @Override
    public boolean test(JavaClass javaClass) {
        return  (!javaClass.isInterface() && !javaClass.getName().endsWith("Impl"));
    }
}
