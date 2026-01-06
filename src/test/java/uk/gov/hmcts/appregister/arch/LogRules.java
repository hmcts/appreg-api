package uk.gov.hmcts.appregister.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

import com.tngtech.archunit.lang.ConditionEvents;

import com.tngtech.archunit.lang.SimpleConditionEvent;

import uk.gov.hmcts.appregister.arch.rules.LoggingCondition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * The rules around logging.
 */
@AnalyzeClasses(packages = BaseRules.BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
public class LogRules extends BaseRules {
    @ArchTest
    static final ArchRule feature_controller_has_logging =
        classes().that().resideInAPackage(BASE_PACKAGE + ".(*).controller..")
            .should(new LoggingCondition());

    @ArchTest
    static final ArchRule feature_service_has_logging =
        classes()
            .that().resideInAPackage(BASE_PACKAGE + ".(*).service..")
            .should(new LoggingCondition());
}
