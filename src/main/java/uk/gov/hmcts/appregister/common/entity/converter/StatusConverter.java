package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

/**
 * Allows us to convert between the Status enum and the database representation and vice versa. The
 * use of Status enum ensures that only valid values are used in entities.
 */
@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Status.fromValue(dbData);
    }
}
