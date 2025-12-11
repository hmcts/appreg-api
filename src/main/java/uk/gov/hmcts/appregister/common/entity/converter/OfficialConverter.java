package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;

@Converter(autoApply = true)
public class OfficialConverter implements AttributeConverter<OfficialType, String> {

    @Override
    public String convertToDatabaseColumn(OfficialType status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public OfficialType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OfficialType.fromValue(dbData);
    }
}
