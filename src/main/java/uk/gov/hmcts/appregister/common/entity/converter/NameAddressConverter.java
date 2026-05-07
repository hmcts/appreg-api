package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;

@Converter(autoApply = true)
public class NameAddressConverter implements AttributeConverter<NameAddressCodeType, String> {
    @Override
    public String convertToDatabaseColumn(NameAddressCodeType status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public NameAddressCodeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NameAddressCodeType.fromCode(dbData);
    }
}
