package uk.gov.hmcts.appregister.common.template;

import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;

/**
 * A class that supports a template which values can be substituted into. Templates are assumed to
 * contain data that supports the validity of values that are substituted.
 */
public interface Templateable {

    /**
     * Gets value for the template.
     *
     * @return The value for the template
     */
    String getValue();

    /**
     * Gets the ket constraint of the template.
     *
     * @return The reference string of the template.
     */
    TemplateKeyWithConstraint getDetail();

    /**
     * Can substitute be performed with the given options.
     *
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException on failure due to data
     *     validation according to length or data type
     */
    void canValueBeSubstituted(String value);

    /**
     * Is substitute complete.
     *
     * @return Is substitute complete
     */
    boolean isSubstitutionComplete();

    /**
     * does value substitute according to the rules.
     *
     * @param value The value to check
     * @return True or false
     */
    default boolean doesSubstitute(String value) {
        try {
            canValueBeSubstituted(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
