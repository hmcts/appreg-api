package uk.gov.hmcts.appregister.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import uk.gov.hmcts.appregister.generated.model.OfficialType;

/**
 * A useful mapper to convert standard applicant to applicant Dto.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public class OfficialMapper {
    public uk.gov.hmcts.appregister.generated.model.OfficialType toOfficial(
        uk.gov.hmcts.appregister.common.enumeration.OfficialType officialType) {
        if (officialType == uk.gov.hmcts.appregister.common.enumeration.OfficialType.CLERK) {
            return uk.gov.hmcts.appregister.generated.model.OfficialType.CLERK;
        } else if (officialType == uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE){
            return uk.gov.hmcts.appregister.generated.model.OfficialType.MAGISTRATE;
        }

        return null;
    }

    public uk.gov.hmcts.appregister.common.enumeration.OfficialType toOfficial(
        uk.gov.hmcts.appregister.generated.model.OfficialType officialType) {
        if (officialType == uk.gov.hmcts.appregister.generated.model.OfficialType.CLERK) {
            return uk.gov.hmcts.appregister.common.enumeration.OfficialType.CLERK;
        } else if (officialType == uk.gov.hmcts.appregister.generated.model.OfficialType.MAGISTRATE){
            return uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE;
        }

        return null;
    }
}
