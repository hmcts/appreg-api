package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.arch.rules.DatabaseClassCondition;
import uk.gov.hmcts.appregister.arch.rules.NoInterfaceOrInnerPredicate;

/**
 * Rules around database.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class DatabaseRules extends BaseRules {
    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule common_entity_format =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity")
                    .and(not(simpleNameEndingWith("_")))
                    .and(not(simpleNameContaining("$")))
                    .should(new DatabaseClassCondition());

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule common_repository_format =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity.repository")
                    .should()
                    .haveSimpleNameEndingWith("Repository")
                    .andShould()
                    .beAssignableTo(JpaRepository.class);

    // 4) Naming conventions (optional but useful)
    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule service_methods_should_contain_transactional =
            methods()
                    .that()
                    .arePublic()
                    .and()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE_PACKAGE + ".(*).service..")
                    .and(new NoInterfaceOrInnerPredicate())
                    .should()
                    .beAnnotatedWith(
                            org.springframework.transaction.annotation.Transactional.class);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule common_converter_format =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity.converter")
                    .should()
                    .haveSimpleNameEndingWith("Converter")
                    .andShould()
                    .beAssignableTo(AttributeConverter.class);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule entities_should_be_in_common =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .and()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule repositories_should_be_in_common =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .and()
                    .areAssignableTo(JpaRepository.class)
                    .should()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity.repository");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule converters_should_be_in_common =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .and()
                    .areAssignableTo(AttributeConverter.class)
                    .should()
                    .resideInAPackage(BASE_PACKAGE + ".common.entity.converter");
}
