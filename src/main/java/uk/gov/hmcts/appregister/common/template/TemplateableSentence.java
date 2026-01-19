package uk.gov.hmcts.appregister.common.template;

import java.util.List;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * A templateable sentence that can have multiple templateable items substituted. This interface
 * represents a collection of all templateable items in the sentence.
 */
public interface TemplateableSentence {

    /**
     * gets contained array of templateable items that have been parsed.
     *
     * @return The templateable array
     */
    Templateable[] getTemplateableContents();

    /**
     * gets the erroneous templates that were found if any.
     *
     * @return list of erroneous template strings
     */
    List<String> getErroneousTemplates();

    /**
     * gets all reference names for all templates in the collection.
     *
     * @return list of all template references in he correct order they were processed
     */
    TemplateDetail getDetail();

    /**
     * Substitutes the templates in the sentence with the provided options.
     *
     * @param values the list of options to substitute into the templates
     * @return The sentence with the templates substituted with the provided values
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException The first error that
     *     is seen
     */
    SubstitutedSentence substitute(List<TemplateSubstitution> values);

    /**
     * Substitutes a single value into a sentence.
     *
     * @param values the templateable item to substitute into
     * @return The sentence with the templates substituted with the provided values. NOTE: If the
     *     template can not be substituted the original sentence is returned
     */
    TemplateableSentence substituteForTemplate(Templateable values, String valueToSubstitute);

    /**
     * Gets the fully substituted string.
     *
     * @return the substituted string so far
     */
    SubstitutedSentence getSubstitutedSentence();

    /**
     * gets an unsubstituted reference key.
     *
     * @param referenceKey The reference key
     * @return Gets the first reference in the collection or null if not found
     */
    Templateable getTemplateForReference(String referenceKey);

    /**
     * gets the keys that need to be substituted.
     *
     * @return The key/value pairs that need to be substituted
     */
    List<TemplateSubstitution> getKeysToBeSubstituted();

    /**
     * Is substitute complete.
     *
     * @return Is substitute complete
     */
    boolean isSubstitutionComplete();
}
