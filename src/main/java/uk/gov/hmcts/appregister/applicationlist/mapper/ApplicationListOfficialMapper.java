package uk.gov.hmcts.appregister.applicationlist.mapper;

import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.generated.model.Official;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
public abstract class ApplicationListOfficialMapper {

    @Autowired OfficialMapper officialMapper;

    public Official toOfficialDto(ApplicationListEntryOfficialPrintProjection printProjection) {
        Official off = new Official();
        off.setSurname(printProjection.getSurname());
        off.setTitle(printProjection.getTitle());
        off.setForename(printProjection.getForename());
        off.setType(officialMapper.toOfficial(printProjection.getType()));
        return off;
    }
}
