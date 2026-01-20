package uk.gov.hmcts.appregister.common.template;

import java.util.List;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * Represents a sentence with substituted values.
 */
public interface SubstitutedSentence {
    /**
     * gets the applied values that were substituted into the sentence.
     *
     * @return The list of applied values
     */
    List<String> getAppliedValues();

    /**
     * applies the template substitution keys with the associated values.
     *
     * @param values the list of template substitution key value pairs that the values will be
     *     applied to
     * @return true or false depending on whether the values were applied
     */
    boolean applyValuesTo(List<TemplateSubstitution> values);

    /**
     * gets the fully substituted string.
     *
     * @return the substituted string so far
     */
    String getSubstitutedString();
}
