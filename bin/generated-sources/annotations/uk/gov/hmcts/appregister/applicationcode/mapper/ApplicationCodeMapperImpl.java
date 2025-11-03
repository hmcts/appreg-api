package uk.gov.hmcts.appregister.applicationcode.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T15:19:56+0000",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ApplicationCodeMapperImpl extends ApplicationCodeMapper {

    @Override
    public ApplicationCodeGetSummaryDto toApplicationCodeGetSummaryDto(ApplicationCode entity, Fee fee, Fee offsiteFee) {
        if ( entity == null && fee == null && offsiteFee == null ) {
            return null;
        }

        ApplicationCodeGetSummaryDto applicationCodeGetSummaryDto = new ApplicationCodeGetSummaryDto();

        if ( entity != null ) {
            if ( entity.getCode() != null ) {
                applicationCodeGetSummaryDto.setApplicationCode( entity.getCode() );
            }
            if ( entity.getTitle() != null ) {
                applicationCodeGetSummaryDto.setTitle( entity.getTitle() );
            }
            if ( entity.getWording() != null ) {
                applicationCodeGetSummaryDto.setWording( entity.getWording() );
            }
            if ( entity.getRequiresRespondent() != null ) {
                applicationCodeGetSummaryDto.setRequiresRespondent( map( entity.getRequiresRespondent() ) );
            }
            if ( entity.getBulkRespondentAllowed() != null ) {
                applicationCodeGetSummaryDto.setBulkRespondentAllowed( map( entity.getBulkRespondentAllowed() ) );
            }
            if ( entity.getFeeReference() != null ) {
                applicationCodeGetSummaryDto.setFeeReference( mapFeeReference( entity.getFeeReference() ) );
            }
            if ( entity.getFeeDue() != null ) {
                applicationCodeGetSummaryDto.setIsFeeDue( map( entity.getFeeDue() ) );
            }
        }
        if ( fee != null ) {
            if ( fee != null ) {
                applicationCodeGetSummaryDto.setFeeAmount( map( fee ) );
            }
            if ( fee.getDescription() != null ) {
                applicationCodeGetSummaryDto.setFeeDescription( map( fee.getDescription() ) );
            }
        }
        if ( offsiteFee != null ) {
            applicationCodeGetSummaryDto.setOffsiteFeeAmount( map( offsiteFee ) );
        }

        return applicationCodeGetSummaryDto;
    }

    @Override
    public ApplicationCodeGetDetailDto toApplicationCodeGetDetailDto(ApplicationCode entity, Fee fee, Fee offsiteFee) {
        if ( entity == null && fee == null && offsiteFee == null ) {
            return null;
        }

        ApplicationCodeGetDetailDto applicationCodeGetDetailDto = new ApplicationCodeGetDetailDto();

        if ( entity != null ) {
            if ( entity.getCode() != null ) {
                applicationCodeGetDetailDto.setApplicationCode( entity.getCode() );
            }
            if ( entity.getTitle() != null ) {
                applicationCodeGetDetailDto.setTitle( entity.getTitle() );
            }
            if ( entity.getWording() != null ) {
                applicationCodeGetDetailDto.setWording( entity.getWording() );
            }
            if ( entity.getRequiresRespondent() != null ) {
                applicationCodeGetDetailDto.setRequiresRespondent( map( entity.getRequiresRespondent() ) );
            }
            if ( entity.getBulkRespondentAllowed() != null ) {
                applicationCodeGetDetailDto.setBulkRespondentAllowed( map( entity.getBulkRespondentAllowed() ) );
            }
            if ( entity.getFeeReference() != null ) {
                applicationCodeGetDetailDto.setFeeReference( mapFeeReference( entity.getFeeReference() ) );
            }
            if ( entity.getStartDate() != null ) {
                applicationCodeGetDetailDto.setStartDate( entity.getStartDate() );
            }
            if ( entity.getEndDate() != null ) {
                applicationCodeGetDetailDto.setEndDate( mapNullableLocalDate( entity.getEndDate() ) );
            }
            if ( entity.getFeeDue() != null ) {
                applicationCodeGetDetailDto.setIsFeeDue( map( entity.getFeeDue() ) );
            }
        }
        if ( fee != null ) {
            if ( fee != null ) {
                applicationCodeGetDetailDto.setFeeAmount( map( fee ) );
            }
            if ( fee.getDescription() != null ) {
                applicationCodeGetDetailDto.setFeeDescription( map( fee.getDescription() ) );
            }
        }
        if ( offsiteFee != null ) {
            applicationCodeGetDetailDto.setOffsiteFeeAmount( map( offsiteFee ) );
        }

        return applicationCodeGetDetailDto;
    }
}
