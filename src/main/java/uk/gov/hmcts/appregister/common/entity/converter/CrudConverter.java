package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Allows us to convert between the YesOrNo enum and the database representation and vice versa. The
 * use of CrudEnum enum ensures that only valid values are used in entities.
 */
@Converter(autoApply = true)
public class CrudConverter implements AttributeConverter<CrudEnum, Character> {

    @Override
    public Character convertToDatabaseColumn(CrudEnum status) {
        return status.getValue();
    }

    @Override
    public CrudEnum convertToEntityAttribute(Character dbData) {
        return CrudEnum.fromValue(dbData);
    }
}
