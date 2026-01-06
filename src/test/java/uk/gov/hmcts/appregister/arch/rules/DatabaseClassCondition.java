package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;

public class DatabaseClassCondition extends ArchCondition<JavaClass> {
    public DatabaseClassCondition() {
        super("entity database check ");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        if (!javaClass.getName().contains("$")) {
            javaClass.getAnnotationOfType(Entity.class);
            javaClass.getAnnotationOfType(Table.class);

            EntityListeners listener = null;
            try {
                listener = javaClass.getAnnotationOfType(EntityListeners.class);
            } catch (IllegalArgumentException e) {
                EntityListeners[] listeners =
                        javaClass
                                .getSuperclass()
                                .get()
                                .getClass()
                                .getAnnotationsByType(EntityListeners.class);
                if (listeners.length > 0) {
                    listener = listeners[0];
                }
            }

            if (listener == null
                    || !listener.value()
                            .getClass()
                            .getCanonicalName()
                            .equals(PreCreateUpdateEntityListener.class.getCanonicalName())) {
                SimpleConditionEvent.violated(
                        javaClass,
                        "Listener annotation on class %s does not have @PreCreateUpdateEntityListener annotation"
                                .formatted(javaClass.getName()));
            }
        }
    }
}
