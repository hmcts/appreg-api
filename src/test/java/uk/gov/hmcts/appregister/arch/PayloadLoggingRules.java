package uk.gov.hmcts.appregister.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.common.log.LogPayloads;

/**
 * Rules around payload logging annotation placement.
 */
@AnalyzeClasses(
        packages = BaseRules.BASE_PACKAGE,
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class PayloadLoggingRules extends BaseRules {
    private static final String GENERATED_MODEL_PACKAGE = BASE_PACKAGE + ".generated.model";

    @ArchTest
    static final ArchRule log_payloads_annotation_only_used_on_supported_controller_methods =
            methods()
                    .that()
                    .areAnnotatedWith(LogPayloads.class)
                    .should(haveSupportedPlacement())
                    .because(
                            "@LogPayloads is only supported on public controller methods that accept a"
                                    + " generated model payload and return ResponseEntity");

    private static ArchCondition<JavaMethod> haveSupportedPlacement() {
        return new ArchCondition<>(
                "be a public controller method returning ResponseEntity and accepting a generated model payload") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                var violations = new ArrayList<String>();

                if (!method.getOwner().getPackageName().contains(".controller")) {
                    violations.add("must be declared in a controller package");
                }

                if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
                    violations.add("must be public");
                }

                if (!method.getRawReturnType().isAssignableTo(ResponseEntity.class)) {
                    violations.add("must return ResponseEntity");
                }

                if (method.getRawParameterTypes().stream()
                        .noneMatch(
                                parameter ->
                                        parameter
                                                .getPackageName()
                                                .startsWith(GENERATED_MODEL_PACKAGE))) {
                    violations.add("must accept at least one generated model payload parameter");
                }

                if (!violations.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    method,
                                    "Method %s.%s annotated with @LogPayloads %s"
                                            .formatted(
                                                    method.getOwner().getSimpleName(),
                                                    method.getName(),
                                                    String.join(", ", violations))));
                }
            }
        };
    }
}
