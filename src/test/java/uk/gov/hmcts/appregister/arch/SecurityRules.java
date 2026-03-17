package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.arch.condition.PreAuthorizeCondition;

/**
 * Rules around security annotations.
 */
@AnalyzeClasses(packages = BaseRules.BASE_PACKAGE)
@Slf4j
public class SecurityRules extends BaseRules {
    @ArchTest
    static final ArchRule feature_pre_authorize_controller_check =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage(BASE_PACKAGE + ".(*).controller..")
                    .andShould(new PreAuthorizeCondition());
}
