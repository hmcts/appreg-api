package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

public class DatabaseClassCondition extends ArchCondition<JavaClass> {
    public DatabaseClassCondition() {
        super("have a preauth check ");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        if (!javaClass.getName().contains("$")) {
            javaClass.getAnnotationOfType(Entity.class);
            javaClass.getAnnotationOfType(Table.class);

        }
    }
}
