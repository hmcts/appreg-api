package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import uk.gov.hmcts.appregister.arch.predicate.NoInterfaceOrInnerPredicate;
import uk.gov.hmcts.appregister.audit.annotation.NestedAudit;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;

/**
 * Rules around nested audit orchestration.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class AuditRules extends BaseRules {
    private static final DescribedPredicate<JavaMethod> OUTER_NESTED_AUDIT_METHOD =
            new DescribedPredicate<>(
                    "method that directly audits and also calls another method that directly audits") {
                @Override
                public boolean test(JavaMethod input) {
                    return isOuterNestedAuditMethod(input);
                }
            };

    @ArchTest
    static final ArchRule outer_nested_audit_methods_must_be_annotated =
            methods()
                    .that(OUTER_NESTED_AUDIT_METHOD)
                    .and()
                    .arePublic()
                    .and()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE_PACKAGE + ".(*).service..")
                    .and(new NoInterfaceOrInnerPredicate())
                    .should()
                    .beAnnotatedWith(NestedAudit.class)
                    .because(
                            "outer methods that orchestrate nested auditing must be explicitly marked"
                                    + " with @NestedAudit");

    private static boolean isOuterNestedAuditMethod(JavaMethod method) {
        return callsProcessAuditDirectly(method)
                && method.getMethodCallsFromSelf().stream()
                        .flatMap(call -> call.getTarget().resolveMember().stream())
                        .anyMatch(AuditRules::callsProcessAuditDirectly);
    }

    private static boolean callsProcessAuditDirectly(JavaMethod method) {
        return method.getMethodCallsFromSelf().stream().anyMatch(AuditRules::isProcessAuditCall);
    }

    private static boolean isProcessAuditCall(JavaMethodCall call) {
        return "processAudit".equals(call.getName())
                && call.getTargetOwner().isAssignableTo(AuditOperationService.class);
    }
}
