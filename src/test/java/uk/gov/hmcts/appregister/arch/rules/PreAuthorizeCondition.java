package uk.gov.hmcts.appregister.arch.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Annotation;

/**
 * A condition to check that a class matches the preauthorize at
 * the class or each on each public method.
 */
@Slf4j
public class PreAuthorizeCondition extends ArchCondition<JavaClass> {
    public PreAuthorizeCondition() {
        super("preauthorize check");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        if (!javaClass.isInterface() && !javaClass.getName().contains("$")) {
            Annotation annotation = null;
            try {
                annotation = javaClass.getAnnotationOfType(PreAuthorize.class);
            } catch (IllegalArgumentException e) {
                log.warn("Class does not contain @PreAuthorize annotation: {}", javaClass.getName());
            }

            // if we dont have the annotation on the class then ensure each method has it
            if (annotation == null) {
                for (JavaMethod method : javaClass.getMethods()) {
                    if (isPublic(method)) {
                        boolean methodAnnotation = method.isAnnotatedWith(PreAuthorize.class);
                        if (!methodAnnotation) {
                            events.add(SimpleConditionEvent.violated(
                                javaClass,
                                "Method %s does not have @PreAuthorize annotation".formatted(javaClass.getName()
                                                                                                 + " "
                                                                                                 + method.getName())
                            ));
                        }
                    }
                }
            }
        }
    }

    private boolean isPublic(JavaMethod javaMethod) {
        return "PUBLIC".equals(javaMethod.getModifiers().iterator().next().name());
    }
}
