package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.appregister.arch.condition.BeanCondition;
import uk.gov.hmcts.appregister.arch.predicate.MethodOwnerNoInterfaceOrInnerPredicate;
import uk.gov.hmcts.appregister.arch.predicate.NoInnerPredicate;

/**
 * Rules around the confohg package.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ConfigRules extends BaseRules {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule config_format =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".config")
                    .and(new NoInnerPredicate())
                    .should()
                    .haveSimpleNameEndingWith("Config")
                    .andShould()
                    .beAnnotatedWith(Configuration.class);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule config_method_bean =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE_PACKAGE + ".config..")
                    .and(new MethodOwnerNoInterfaceOrInnerPredicate())
                    .should(new BeanCondition())
                    .allowEmptyShould(true);
}
