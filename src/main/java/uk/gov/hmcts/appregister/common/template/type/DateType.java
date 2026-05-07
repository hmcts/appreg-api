package uk.gov.hmcts.appregister.common.template.type;

public class DateType implements DataType {
    @Override
    public boolean validateForType(String value) {
        // TODO: Re-enable this once the decision has been made on the FE implementation.
        //        try {
        //            LocalDate.parse(value);
        //            return true;
        //        } catch (DateTimeParseException dateTimeParseException) {
        //            return false;
        //        }
        return true;
    }
}
