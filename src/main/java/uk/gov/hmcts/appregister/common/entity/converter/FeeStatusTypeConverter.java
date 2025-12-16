package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

@Converter(autoApply = true)
public class FeeStatusTypeConverter implements AttributeConverter<FeeStatusType, String> {

    @Override
    public String convertToDatabaseColumn(FeeStatusType status) {
        return status == null ? null : status.getDisplayName();
    }

    @Override
    public FeeStatusType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FeeStatusType.fromValue(dbData);
    }
}
