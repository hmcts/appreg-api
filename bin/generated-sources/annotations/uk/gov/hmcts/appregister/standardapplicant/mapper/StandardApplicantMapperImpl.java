package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T15:19:56+0000",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class StandardApplicantMapperImpl implements StandardApplicantMapper {

    @Override
    public StandardApplicantDto toReadDto(StandardApplicant entity) {
        if ( entity == null ) {
            return null;
        }

        String applicantName = null;
        Long id = null;
        String applicantCode = null;
        String applicantTitle = null;
        String applicantForename1 = null;
        String applicantForename2 = null;
        String applicantForename3 = null;
        String applicantSurname = null;
        String addressLine1 = null;
        String addressLine2 = null;
        String addressLine3 = null;
        String addressLine4 = null;
        String addressLine5 = null;
        String postcode = null;
        String emailAddress = null;
        String telephoneNumber = null;
        String mobileNumber = null;
        LocalDate applicantStartDate = null;
        LocalDate applicantEndDate = null;

        if ( entity.getName() != null ) {
            applicantName = entity.getName();
        }
        if ( entity.getId() != null ) {
            id = entity.getId();
        }
        if ( entity.getApplicantCode() != null ) {
            applicantCode = entity.getApplicantCode();
        }
        if ( entity.getApplicantTitle() != null ) {
            applicantTitle = entity.getApplicantTitle();
        }
        if ( entity.getApplicantForename1() != null ) {
            applicantForename1 = entity.getApplicantForename1();
        }
        if ( entity.getApplicantForename2() != null ) {
            applicantForename2 = entity.getApplicantForename2();
        }
        if ( entity.getApplicantForename3() != null ) {
            applicantForename3 = entity.getApplicantForename3();
        }
        if ( entity.getApplicantSurname() != null ) {
            applicantSurname = entity.getApplicantSurname();
        }
        if ( entity.getAddressLine1() != null ) {
            addressLine1 = entity.getAddressLine1();
        }
        if ( entity.getAddressLine2() != null ) {
            addressLine2 = entity.getAddressLine2();
        }
        if ( entity.getAddressLine3() != null ) {
            addressLine3 = entity.getAddressLine3();
        }
        if ( entity.getAddressLine4() != null ) {
            addressLine4 = entity.getAddressLine4();
        }
        if ( entity.getAddressLine5() != null ) {
            addressLine5 = entity.getAddressLine5();
        }
        if ( entity.getPostcode() != null ) {
            postcode = entity.getPostcode();
        }
        if ( entity.getEmailAddress() != null ) {
            emailAddress = entity.getEmailAddress();
        }
        if ( entity.getTelephoneNumber() != null ) {
            telephoneNumber = entity.getTelephoneNumber();
        }
        if ( entity.getMobileNumber() != null ) {
            mobileNumber = entity.getMobileNumber();
        }
        if ( entity.getApplicantStartDate() != null ) {
            applicantStartDate = entity.getApplicantStartDate();
        }
        if ( entity.getApplicantEndDate() != null ) {
            applicantEndDate = entity.getApplicantEndDate();
        }

        StandardApplicantDto standardApplicantDto = new StandardApplicantDto( id, applicantCode, applicantTitle, applicantName, applicantForename1, applicantForename2, applicantForename3, applicantSurname, addressLine1, addressLine2, addressLine3, addressLine4, addressLine5, postcode, emailAddress, telephoneNumber, mobileNumber, applicantStartDate, applicantEndDate );

        return standardApplicantDto;
    }
}
