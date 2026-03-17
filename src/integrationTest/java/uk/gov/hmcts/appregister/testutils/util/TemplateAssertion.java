package uk.gov.hmcts.appregister.testutils.util;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public class TemplateAssertion {
    /**
     * Asserts the basic template structure is present.
     *
     * @param template The template to expect
     * @param templateDetail The template detail to assert against
     */
    public static void assertTemplate(String template, TemplateDetail templateDetail) {
        Assertions.assertNotNull(templateDetail.getTemplate());
        Assertions.assertEquals(template, templateDetail.getTemplate());

        // make the check on the substitution
        for (TemplateKeyWithConstraint constraint :
                templateDetail.getSubstitutionKeyConstraints()) {
            Assertions.assertNotNull(constraint.getConstraint().getType());
            Assertions.assertNotNull(constraint.getConstraint().getLength());
            Assertions.assertNull(constraint.getValue());
            Assertions.assertNotNull(constraint.getKey());
        }
    }

    /**
     * Asserts the template values are applied.
     *
     * @param wordingTemplate The template to expect
     * @param substitutions The substitution values to expect
     * @param templateDetail The template detail to assert against
     */
    public static void assertTemplateWithValues(
            String wordingTemplate,
            List<TemplateSubstitution> substitutions,
            TemplateDetail templateDetail) {
        Assertions.assertNotNull(templateDetail.getTemplate());
        Assertions.assertEquals(wordingTemplate, templateDetail.getTemplate());

        if (substitutions != null) {
            Assertions.assertEquals(
                    substitutions.size(), templateDetail.getSubstitutionKeyConstraints().size());

            // make the check on the substitution
            for (TemplateSubstitution substitution : substitutions) {
                templateDetail.getSubstitutionKeyConstraints().stream()
                        .filter(s -> s.getKey().equals(substitution.getKey()))
                        .findFirst()
                        .ifPresentOrElse(
                                s -> {
                                    Assertions.assertNotNull(s.getKey());
                                    Assertions.assertNotNull(s.getConstraint());
                                    Assertions.assertNotNull(s.getConstraint().getType());
                                    Assertions.assertNotNull(s.getConstraint().getLength());
                                    Assertions.assertEquals(substitution.getValue(), s.getValue());
                                },
                                () ->
                                        Assertions.fail(
                                                "Expected substitution key not found: "
                                                        + substitution.getKey()));
            }
        }
    }
}
