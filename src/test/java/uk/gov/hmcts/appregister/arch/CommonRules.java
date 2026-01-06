package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import uk.gov.hmcts.appregister.arch.rules.NoEndingWithImplPredicate;

/**
 * Rules around common package.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class CommonRules extends BaseRules {
    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule common_enum =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".common.enumeration")
                    .should()
                    .beAssignableTo(Enum.class);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule
            repositories_should_be_in_persistence_and_end_with_repository =
                    classes()
                            .that()
                            .resideInAPackage(BASE_PACKAGE + ".common.mapper..")
                            .and(new NoEndingWithImplPredicate())
                            .should()
                            .haveSimpleNameEndingWith("Mapper");
}
