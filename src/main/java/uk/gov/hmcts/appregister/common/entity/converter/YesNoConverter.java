package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

/**
 * Allows us to convert between the YesOrNo enum and the database representation and vice versa. The
 * use of YesOrNo enum ensures that only valid values are used in entities.
 */
@Converter(autoApply = true)
public class YesNoConverter implements AttributeConverter<YesOrNo, String> {

    @Override
    public String convertToDatabaseColumn(YesOrNo status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public YesOrNo convertToEntityAttribute(String dbData) {
        return dbData == null ? null : YesOrNo.fromValue(dbData);
    }
}
