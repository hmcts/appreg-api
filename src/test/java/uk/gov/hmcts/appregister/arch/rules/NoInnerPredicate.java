package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Predicate to match classes that are not inner classes or interfaces.
 */
public class NoInnerPredicate extends DescribedPredicate<JavaClass> {
    public NoInnerPredicate() {
        super("no interface or inner class");
    }

    @Override
    public boolean test(JavaClass javaClass) {
        return  (!javaClass.isInterface() && !javaClass.getName().contains("$"));
    }
}
