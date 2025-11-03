package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T15:19:55+0000",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class CriminalJusticeMapperImpl implements CriminalJusticeMapper {

    @Override
    public CriminalJusticeAreaGetDto toDto(CriminalJusticeArea criminalJusticeArea) {
        if ( criminalJusticeArea == null ) {
            return null;
        }

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();

        criminalJusticeAreaGetDto.setCode( criminalJusticeArea.getCode() );
        criminalJusticeAreaGetDto.setDescription( criminalJusticeArea.getDescription() );

        return criminalJusticeAreaGetDto;
    }
}
