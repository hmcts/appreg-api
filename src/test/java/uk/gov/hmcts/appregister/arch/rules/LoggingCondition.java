package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Does a class contain the slf logger.
 */
public class LoggingCondition extends ArchCondition<JavaClass> {
    public LoggingCondition() {
        super("logging field check");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        // ignore inner classes and interfaces
        if (!javaClass.isInterface() && !javaClass.getName().contains("$")) {

            // check for the log field which is the standard name for slf logger
            boolean hasLogField =
                    javaClass.getFields().stream().anyMatch(f -> f.getName().equals("log"));

            if (!hasLogField) {
                events.add(
                        SimpleConditionEvent.violated(
                                javaClass,
                                "Controller %s does not declare a field named 'log'"
                                        .formatted(javaClass.getName())));
            }
        }
    }
}
