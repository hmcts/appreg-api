package uk.gov.hmcts.appregister.applicationlist.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T15:19:55+0000",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ApplicationListMapperImpl implements ApplicationListMapper {

    @Override
    public ApplicationList toCreateEntityWithCourt(ApplicationListCreateDto dto, NationalCourtHouse court) {
        if ( dto == null && court == null ) {
            return null;
        }

        ApplicationList.ApplicationListBuilder applicationList = ApplicationList.builder();

        if ( dto != null ) {
            applicationList.description( dto.getDescription() );
            applicationList.date( dto.getDate() );
            applicationList.time( dto.getTime() );
            if ( dto.getDurationHours() != null ) {
                applicationList.durationHours( dto.getDurationHours().shortValue() );
            }
            if ( dto.getDurationMinutes() != null ) {
                applicationList.durationMinutes( dto.getDurationMinutes().shortValue() );
            }
            applicationList.status( dto.getStatus() );
        }
        if ( court != null ) {
            applicationList.courtCode( court.getCourtLocationCode() );
            applicationList.courtName( court.getName() );
        }

        return applicationList.build();
    }

    @Override
    public ApplicationList toCreateEntityWithCja(ApplicationListCreateDto dto, CriminalJusticeArea cja) {
        if ( dto == null && cja == null ) {
            return null;
        }

        ApplicationList.ApplicationListBuilder applicationList = ApplicationList.builder();

        if ( dto != null ) {
            applicationList.otherLocation( dto.getOtherLocationDescription() );
            applicationList.description( dto.getDescription() );
            applicationList.date( dto.getDate() );
            applicationList.time( dto.getTime() );
            if ( dto.getDurationHours() != null ) {
                applicationList.durationHours( dto.getDurationHours().shortValue() );
            }
            if ( dto.getDurationMinutes() != null ) {
                applicationList.durationMinutes( dto.getDurationMinutes().shortValue() );
            }
            applicationList.status( dto.getStatus() );
        }
        applicationList.cja( cja );

        return applicationList.build();
    }

    @Override
    public ApplicationListGetDetailDto toGetDetailDto(ApplicationList appList, CriminalJusticeArea cja) {
        if ( appList == null && cja == null ) {
            return null;
        }

        ApplicationListGetDetailDto applicationListGetDetailDto = new ApplicationListGetDetailDto();

        if ( appList != null ) {
            applicationListGetDetailDto.setId( appList.getUuid() );
            applicationListGetDetailDto.setDate( appList.getDate() );
            applicationListGetDetailDto.setTime( appList.getTime() );
            applicationListGetDetailDto.setDescription( appList.getDescription() );
            applicationListGetDetailDto.setStatus( appList.getStatus() );
            applicationListGetDetailDto.setCourtCode( appList.getCourtCode() );
            applicationListGetDetailDto.setCourtName( appList.getCourtName() );
            applicationListGetDetailDto.setOtherLocationDescription( appList.getOtherLocation() );
            applicationListGetDetailDto.setDurationHours( (int) appList.getDurationHours() );
            applicationListGetDetailDto.setDurationMinutes( (int) appList.getDurationMinutes() );
            applicationListGetDetailDto.setVersion( appList.getVersion() );
        }
        applicationListGetDetailDto.setCjaCode( cja != null ? cja.getCode() : null );

        return applicationListGetDetailDto;
    }

    @Override
    public ApplicationListGetSummaryDto toGetSummaryDto(ApplicationList appList, long entryCount, String location) {
        if ( appList == null && location == null ) {
            return null;
        }

        ApplicationListGetSummaryDto applicationListGetSummaryDto = new ApplicationListGetSummaryDto();

        if ( appList != null ) {
            applicationListGetSummaryDto.setId( appList.getUuid() );
            applicationListGetSummaryDto.setDate( appList.getDate() );
            applicationListGetSummaryDto.setTime( appList.getTime() );
            applicationListGetSummaryDto.setDescription( appList.getDescription() );
            applicationListGetSummaryDto.setStatus( appList.getStatus() );
        }
        applicationListGetSummaryDto.setNumberOfEntries( (int) entryCount );
        applicationListGetSummaryDto.setLocation( location );

        return applicationListGetSummaryDto;
    }
}
