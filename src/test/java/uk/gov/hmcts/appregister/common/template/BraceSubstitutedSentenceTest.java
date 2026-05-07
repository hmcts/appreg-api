package uk.gov.hmcts.appregister.common.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BraceSubstitutedSentenceTest {
    @Test
    public void testBraceParse() {
        String valueTemplate =
                "Test template {office1Val} and second template "
                        + "{office2Val} and third\" +\n"
                        + "                            \"template {office3Val}";

        Assertions.assertEquals(
                "office1Val",
                BraceSubstitutedSentence.withSubstitutedSentence(valueTemplate)
                        .getAppliedValues()
                        .get(0));
        Assertions.assertEquals(
                "office2Val",
                BraceSubstitutedSentence.withSubstitutedSentence(valueTemplate)
                        .getAppliedValues()
                        .get(1));
        Assertions.assertEquals(
                "office3Val",
                BraceSubstitutedSentence.withSubstitutedSentence(valueTemplate)
                        .getAppliedValues()
                        .get(2));
    }
}
