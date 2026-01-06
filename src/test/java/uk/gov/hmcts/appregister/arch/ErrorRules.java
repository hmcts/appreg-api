package uk.gov.hmcts.appregister.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import com.tngtech.archunit.lang.ArchRule;

import uk.gov.hmcts.appregister.arch.rules.ExceptionCondition;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.base.DescribedPredicate.not;

/**
 * Rules around the errors.
 */
@AnalyzeClasses(packages = BaseRules.BASE_PACKAGE,  importOptions = { ImportOption.DoNotIncludeTests.class })
public class ErrorRules extends BaseRules {

    @ArchTest
    static final ArchRule all_exceptions_classes_need_to_use_correct_base =
        classes()
            .that().resideInAPackage(BASE_PACKAGE + "..")
            .and().areAssignableTo(Exception.class)
            .should(new ExceptionCondition());


    @ArchTest
    public static final com.tngtech.archunit.lang.ArchRule feature_exception_enum_formatting =
        classes()
            .that().resideInAPackage(BASE_PACKAGE + ".(*).exception..")
            .and(not(resideInAPackage(BASE_PACKAGE + "..common..")))
            .should().haveSimpleNameEndingWith("Error")
            .andShould().implement(ErrorCodeEnum.class).andShould().beAssignableTo(Enum.class);

}
