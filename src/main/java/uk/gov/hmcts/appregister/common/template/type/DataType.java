package uk.gov.hmcts.appregister.common.template.type;

/**
 * This interface represents a wording data type. It ensures that a value
 * conforms to a specific type.
 */
@FunctionalInterface
public interface DataType {
    /**
     * Gets the string for the type implementation.
     * @param value the field to get the string for
     * @return True or false
     */
    boolean validateForType(String value);
}
