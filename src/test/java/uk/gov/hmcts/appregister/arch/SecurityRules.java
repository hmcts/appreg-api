package uk.gov.hmcts.appregister.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;

import uk.gov.hmcts.appregister.arch.rules.PreAuthorizeCondition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Rules around security annotations.
 */
@AnalyzeClasses(packages = BaseRules.BASE_PACKAGE)
@Slf4j
public class SecurityRules extends BaseRules {
    @ArchTest
    static final ArchRule feature_pre_authorize_controller_check =
            classes()
                        .that().haveSimpleNameEndingWith("Controller")
                        .should().resideInAPackage(BASE_PACKAGE + ".(*).controller..")
                        .andShould(new PreAuthorizeCondition());
}
