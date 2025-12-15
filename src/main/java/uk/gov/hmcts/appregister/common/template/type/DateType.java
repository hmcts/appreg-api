package uk.gov.hmcts.appregister.common.template.type;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateType implements DataType {
    @Override
    public boolean validateForType(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException dateTimeParseException) {
            return false;
        }
    }
}
