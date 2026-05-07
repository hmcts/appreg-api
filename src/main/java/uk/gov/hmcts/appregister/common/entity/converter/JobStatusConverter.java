package uk.gov.hmcts.appregister.common.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;

@Converter(autoApply = true)
public class JobStatusConverter implements AttributeConverter<JobStatusType, String> {

    @Override
    public String convertToDatabaseColumn(JobStatusType status) {
        return status == null ? null : status.getState();
    }

    @Override
    public JobStatusType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : JobStatusType.fromStateString(dbData);
    }
}
