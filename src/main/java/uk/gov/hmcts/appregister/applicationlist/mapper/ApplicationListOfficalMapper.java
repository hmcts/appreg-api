package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.appregister.common.projection.ApplicationListOfficialPrintProjection;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ApplicationListOfficalMapper {

    Logger LOG = LoggerFactory.getLogger(ApplicationListOfficalMapper.class);

    Official toOfficialDto(ApplicationListOfficialPrintProjection printProjection);

    default OfficialType mapOfficialType(String code) {
        if (code == null) {
            LOG.warn("Received null official type code. Defaulting to MAGISTRATE.");
            return OfficialType.MAGISTRATE;
        }

        return switch (code) {
            case "M" -> OfficialType.MAGISTRATE;
            case "C" -> OfficialType.CLERK;
            default -> {
                LOG.warn("Invalid official type code: {}. Defaulting to MAGISTRATE.", code);
                yield OfficialType.MAGISTRATE;
            }
        };
    }
}
