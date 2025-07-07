package uk.gov.hmcts.appregister.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.read.IdentityDetailsDto;
import uk.gov.hmcts.appregister.dto.write.IdentityDetailsWriteDto;
import uk.gov.hmcts.appregister.model.IdentityDetails;

@Component
public class IdentityDetailsMapper {

    public IdentityDetailsDto toReadDto(IdentityDetails entity) {
        return new IdentityDetailsDto(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getTitle(),
            entity.getForename1(),
            entity.getForename2(),
            entity.getForename3(),
            entity.getSurname(),
            entity.getAddressLine1(),
            entity.getAddressLine2(),
            entity.getAddressLine3(),
            entity.getAddressLine4(),
            entity.getAddressLine5(),
            entity.getPostcode(),
            entity.getEmailAddress(),
            entity.getTelephoneNumber(),
            entity.getMobileNumber(),
            entity.getDateOfBirth()
        );
    }

    public IdentityDetails createFromWriteDto(IdentityDetailsWriteDto dto) {
        return IdentityDetails.builder()
            .code(dto.code())
            .name(dto.name())
            .title(dto.title())
            .forename1(dto.forename1())
            .forename2(dto.forename2())
            .forename3(dto.forename3())
            .surname(dto.surname())
            .addressLine1(dto.addressLine1())
            .addressLine2(dto.addressLine2())
            .addressLine3(dto.addressLine3())
            .addressLine4(dto.addressLine4())
            .addressLine5(dto.addressLine5())
            .postcode(dto.postcode())
            .emailAddress(dto.emailAddress())
            .telephoneNumber(dto.telephoneNumber())
            .mobileNumber(dto.mobileNumber())
            .dateOfBirth(dto.dateOfBirth())
            .build();
    }

    public void updateFromWriteDto(IdentityDetailsWriteDto dto, IdentityDetails entity) {
        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setTitle(dto.title());
        entity.setForename1(dto.forename1());
        entity.setForename2(dto.forename2());
        entity.setForename3(dto.forename3());
        entity.setSurname(dto.surname());
        entity.setAddressLine1(dto.addressLine1());
        entity.setAddressLine2(dto.addressLine2());
        entity.setAddressLine3(dto.addressLine3());
        entity.setAddressLine4(dto.addressLine4());
        entity.setAddressLine5(dto.addressLine5());
        entity.setPostcode(dto.postcode());
        entity.setEmailAddress(dto.emailAddress());
        entity.setTelephoneNumber(dto.telephoneNumber());
        entity.setMobileNumber(dto.mobileNumber());
        entity.setDateOfBirth(dto.dateOfBirth());
    }
}
