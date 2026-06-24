package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.mapper.OutgoingDtoSanitiser;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.generated.model.Official;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListOfficialMapper {

    private OfficialMapper officialMapper;

    @Autowired
    public void setOfficialMapper(OfficialMapper officialMapper) {
        this.officialMapper = officialMapper;
    }

    public Official toOfficialDto(ApplicationListEntryOfficialPrintProjection printProjection) {
        Official off = new Official();
        off.setSurname(OutgoingDtoSanitiser.emptyToNull(printProjection.getSurname()));
        off.setTitle(OutgoingDtoSanitiser.emptyToNull(printProjection.getTitle()));
        off.setForename(OutgoingDtoSanitiser.emptyToNull(printProjection.getForename()));
        off.setType(officialMapper.toOfficial(printProjection.getType()));
        return off;
    }
}
