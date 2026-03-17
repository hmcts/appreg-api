package uk.gov.hmcts.appregister.arch.condition;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Matches on an exception extending AppRegistryException or being AppRegistryException.
 */
public class ExceptionCondition extends ArchCondition<JavaClass> {
    public ExceptionCondition() {
        super("AppRegistryException check");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        if (!javaClass.getSuperclass().get().equals(AppRegistryException.class)
                && !javaClass.getFullName().equals(AppRegistryException.class.getCanonicalName())) {
            events.add(
                    SimpleConditionEvent.violated(
                            javaClass,
                            "Class %s does not extend AppRegistryException annotation"
                                    .formatted(javaClass.getName())));
        }
    }
}
