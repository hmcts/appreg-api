package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.util.OfficialTypeUtil;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListOfficialMapper {

    public Official toOfficialDto(ApplicationListEntryOfficialPrintProjection printProjection) {
            Official off = new Official();
            off.setSurname(printProjection.getSurname());
            off.setTitle(printProjection.getTitle());
            off.setForename(printProjection.getForename());
            off.setType(getOfficial(printProjection.getType()));
            return off;
    }

    public OfficialType mapOfficialType(String code) {
        return OfficialTypeUtil.fromCode(code);
    }

    public uk.gov.hmcts.appregister.generated.model.OfficialType getOfficial(uk.gov.hmcts.appregister.common.enumeration.OfficialType officialType) {
        return uk.gov.hmcts.appregister.generated.model.OfficialType.valueOf(officialType.getValue());
    }
}
