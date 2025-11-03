package uk.gov.hmcts.appregister.courtlocation.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T15:19:56+0000",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class CourtLocationMapperImpl implements CourtLocationMapper {

    @Override
    public CourtLocationGetDetailDto toDetailDto(NationalCourtHouse entity) {
        if ( entity == null ) {
            return null;
        }

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();

        if ( entity.getName() != null ) {
            courtLocationGetDetailDto.setName( entity.getName() );
        }
        if ( entity.getCourtLocationCode() != null ) {
            courtLocationGetDetailDto.setLocationCode( entity.getCourtLocationCode() );
        }
        if ( entity.getStartDate() != null ) {
            courtLocationGetDetailDto.setStartDate( entity.getStartDate() );
        }
        if ( entity.getEndDate() != null ) {
            courtLocationGetDetailDto.setEndDate( map( entity.getEndDate() ) );
        }

        return courtLocationGetDetailDto;
    }

    @Override
    public CourtLocationGetSummaryDto toSummaryDto(NationalCourtHouse entity) {
        if ( entity == null ) {
            return null;
        }

        CourtLocationGetSummaryDto courtLocationGetSummaryDto = new CourtLocationGetSummaryDto();

        if ( entity.getName() != null ) {
            courtLocationGetSummaryDto.setName( entity.getName() );
        }
        if ( entity.getCourtLocationCode() != null ) {
            courtLocationGetSummaryDto.setLocationCode( entity.getCourtLocationCode() );
        }

        return courtLocationGetSummaryDto;
    }
}
