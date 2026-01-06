package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.dependencies.SliceRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.arch.rules.NoInnerPredicate;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * The layered architecture rules for each feature package.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayeredRules extends BaseRules {
    @ArchTest
    public static final com.tngtech.archunit.lang.ArchRule feature_internal_layers =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("api")
                    .definedBy(BASE_PACKAGE + ".(*).api..")
                    .layer("audit")
                    .definedBy(BASE_PACKAGE + ".(*).audit..")
                    .layer("controller")
                    .definedBy(BASE_PACKAGE + ".(*).controller..")
                    .layer("mapper")
                    .definedBy(BASE_PACKAGE + ".(*).mapper..")
                    .layer("service")
                    .definedBy(BASE_PACKAGE + ".(*).service..")
                    .layer("validator")
                    .definedBy(BASE_PACKAGE + ".(*).validator..")
                    .layer("common")
                    .definedBy(BASE_PACKAGE + ".common..")
                    .layer("exception")
                    .definedBy(BASE_PACKAGE + ".(*).exception..")
                    .layer("generated")
                    .definedBy(BASE_PACKAGE + ".generated..")
                    .layer("model")
                    .definedBy(BASE_PACKAGE + ".(*).model..")
                    .whereLayer("api")
                    .mayOnlyAccessLayers("common", "service", "generated")
                    .whereLayer("service")
                    .mayOnlyAccessLayers(
                            "model",
                            "generated",
                            "audit",
                            "common",
                            "mapper",
                            "validator",
                            "exception")
                    .whereLayer("validator")
                    .mayOnlyAccessLayers("common", "generated", "exception", "model")

                    // ignore the third parties when assessing dependencies
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."), resideInAPackage("org.slf4j.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."), resideInAPackage("java.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."),
                            resideInAPackage("org.springframework.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."), resideInAPackage("jakarta.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."), resideInAPackage("io.swagger.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."),
                            resideInAPackage("org.mapstruct.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."),
                            resideInAPackage("org.openapitools.."))
                    .ignoreDependency(
                            resideInAPackage(BASE_PACKAGE + ".."), resideInAPackage("lombok.."));

    // TODO: We need to correct the sort api classes before enabling this rule
    /*
    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule api_format =
        classes()
            .that().resideInAPackage(BASE_PACKAGE + ".(*).api..")
            .and(
                not(resideInAnyPackage(BASE_PACKAGE + ".generated.."))
            )
            .and(
                not(resideInAnyPackage(BASE_PACKAGE + ".common.."))
            )
            .should().haveSimpleNameEndingWith("SortFieldEnum")
            .andShould().implement(SortableOperationEnum.class).should()
            .beAssignableTo(Enum.class).allowEmptyShould(true);
            */

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule controllers_format_rule =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".(*).controller..")
                    .should()
                    .haveSimpleNameEndingWith("Controller")
                    .andShould()
                    .beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule service_format_rule =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".(*).service..")
                    .and(new NoInnerPredicate())
                    .should()
                    .haveSimpleNameContaining("Service")
                    .andShould()
                    .beAnnotatedWith(Service.class);

    @ArchTest
    public static final com.tngtech.archunit.lang.ArchRule feature_validator_formatting =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".(*).validator..")
                    .and(new NoInnerPredicate())
                    .and(not(simpleNameEndingWith("Success")))
                    .and(not(resideInAPackage(BASE_PACKAGE + "..common..")))
                    .should()
                    .beAssignableTo(Validator.class)
                    .andShould()
                    .haveSimpleNameEndingWith("Validator");

    // TODO: We need to correct the mapper classes before enabling this rule
    /*
    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule repositories_should_be_in_persistence_and_end_with_repository =
        classes()
            .that().resideInAPackage(BASE_PACKAGE + ".(*).mapper..")
            .and(
                not(resideInAnyPackage(BASE_PACKAGE + ".common.."))
            ).and(new IgnoreImplClass())
            .should().haveSimpleNameEndingWith("Mapper")
            .andShould().beAnnotatedWith(Mapper.class);
    */

    @ArchTest
    static final SliceRule no_depends_between_features =
            SlicesRuleDefinition.slices()
                    .matching(BASE_PACKAGE + ".(*)")
                    // Ignore third party dependencies
                    .should()
                    .notDependOnEachOther()
                    .ignoreDependency(
                            resideInAnyPackage(BASE_PACKAGE + ".(*).."),
                            resideInAnyPackage(BASE_PACKAGE + ".common.."))
                    .ignoreDependency(resideInAnyPackage(BASE_PACKAGE + ".common.."), alwaysTrue())
                    .ignoreDependency(
                            resideInAnyPackage(BASE_PACKAGE + ".(*).."),
                            resideInAnyPackage(BASE_PACKAGE + ".generated.."))
                    .ignoreDependency(resideInAnyPackage(BASE_PACKAGE + ".data.."), alwaysTrue())
                    .ignoreDependency(
                            resideInAnyPackage(BASE_PACKAGE + ".(*).mapper.."), alwaysTrue());
}
