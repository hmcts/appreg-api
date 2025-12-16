package uk.gov.hmcts.appregister.common.template;

import uk.gov.hmcts.appregister.common.template.type.DataType;

/**
 * A class that supports a template which values can be substituted into. Templates are assumed to
 * contain data that supports the validity of values that are substituted.
 *
 * <p>1) Reference string 2) Data type e.g. text, date, integer etc 3) Length.
 */
public interface Templateable {

    /**
     * Gets the reference string of the template.
     *
     * @return The reference string of the template.
     */
    String getReference();

    /** Substitutes the given value into the template. */
    String substitute(String value);

    /**
     * Can substitute be performed with the given options.
     *
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException on failure due to data
     *     validation according to length or data type
     */
    void canValueBeSubstituted(String value);

    default boolean doesSubstitute(String value) {
        try {
            canValueBeSubstituted(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * gets the type of the template.
     *
     * @return The type of the template
     */
    DataType getType();

    /**
     * The length of the template restriction.
     *
     * @return The length restriction
     */
    Integer getLength();
}
