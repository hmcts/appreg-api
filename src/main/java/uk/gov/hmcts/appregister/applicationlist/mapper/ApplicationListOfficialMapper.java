package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.util.OfficialTypeUtil;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListOfficialMapper {

    @Autowired
    OfficialMapper officialMapper;

    public Official toOfficialDto(ApplicationListEntryOfficialPrintProjection printProjection) {
        Official off = new Official();
        off.setSurname(printProjection.getSurname());
        off.setTitle(printProjection.getTitle());
        off.setForename(printProjection.getForename());
        off.setType(officialMapper.toOfficial(printProjection.getType()));
        return off;
    }
}
