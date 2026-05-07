package uk.gov.hmcts.appregister.arch.condition;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.context.annotation.Bean;

/**
 * Condition to match on a method bean annotation (see {@link Bean} or override annotation ({@link
 * Override}).
 */
public class BeanCondition extends ArchCondition<JavaMethod> {
    public BeanCondition() {
        super("have a preauth check ");
    }

    @Override
    public void check(JavaMethod item, ConditionEvents events) {
        boolean classAnnotation = item.isAnnotatedWith(Bean.class);

        if (!classAnnotation) {
            boolean methodAnnotation = item.getOwner().getSuperclass().isPresent();
            if (!methodAnnotation) {
                events.add(
                        SimpleConditionEvent.violated(
                                item,
                                "Method %s does not have @Bean OR @Override annotation"
                                        .formatted(
                                                item.getOwner().getName() + " " + item.getName())));
            }
        }
    }
}
