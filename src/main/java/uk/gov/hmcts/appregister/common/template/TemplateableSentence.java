package uk.gov.hmcts.appregister.common.template;

import java.util.List;

/**
 * A templateable sentence that can have its multiple templateable items substituted. This interface
 * represents is a collection of all templateable items in the sentence.
 */
public interface TemplateableSentence extends List<Templateable> {

    /**
     * gets the erroneous templates that were found if any
     *
     * @return list of erroneous template strings
     */
    List<String> getErroneousTemplates();

    /**
     * gets all reference names for all templates in the collection
     *
     * @return list of all template references in he correct order they were processed
     */
    List<String> getReferences();

    /**
     * Substitutes the templates in the sentence with the provided options.
     *
     * @param values the list of options to substitute into the templates
     * @return The sentence with the templates substituted with the provided values
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException The first error that
     *     is seen
     */
    String substitute(List<String> values);

    /**
     * Substitutes a single value into a sentence
     *
     * @param values the templateable item to substitute into
     * @return The sentence with the templates substituted with the provided values. NOTE: If the
     *     template can not be substituted the original sentence is returned
     */
    TemplateableSentence substituteForTemplate(Templateable values, String valueToSubstitute);

    /**
     * Gets the fully substituted string
     *
     * @return the substituted string so far
     */
    String getSubstitutedSentence();

    /**
     * gets the template by the reference
     *
     * @param referenceValue The reference
     * @return Gets the first reference in the collection if multiple exist
     */
    Templateable getTemplateForReference(String referenceValue);

    /**
     * The sentence template with placeholders for the templates.
     *
     * @return the template with placeholders
     */
    String getSentenceTemplate();
}
