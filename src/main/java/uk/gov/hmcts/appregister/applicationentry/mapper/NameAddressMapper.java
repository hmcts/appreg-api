package uk.gov.hmcts.appregister.applicationentry.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.dto.IdentityDetailsDto;
import uk.gov.hmcts.appregister.applicationentry.dto.IdentityDetailsWriteDto;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

/** Mapper for converting between NameAddress entity and its DTOs. */
@Component
public class NameAddressMapper {

    public IdentityDetailsDto toReadDto(NameAddress entity) {
        return new IdentityDetailsDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getTitle(),
                entity.getForename1(),
                entity.getForename2(),
                entity.getForename3(),
                entity.getSurname(),
                entity.getAddress1(),
                entity.getAddress2(),
                entity.getAddress3(),
                entity.getAddress4(),
                entity.getAddress5(),
                entity.getPostcode(),
                entity.getEmailAddress(),
                entity.getTelephoneNumber(),
                entity.getTelephoneNumber(),
                entity.getDateOfBirth());
    }

    public NameAddress createFromWriteDto(IdentityDetailsWriteDto dto) {
        return NameAddress.builder()
                .code(dto.code())
                .name(dto.name())
                .title(dto.title())
                .forename1(dto.forename1())
                .forename2(dto.forename2())
                .forename3(dto.forename3())
                .surname(dto.surname())
                .address1(dto.addressLine1())
                .address2(dto.addressLine2())
                .address3(dto.addressLine3())
                .address4(dto.addressLine4())
                .address5(dto.addressLine5())
                .postcode(dto.postcode())
                .emailAddress(dto.emailAddress())
                .telephoneNumber(dto.telephoneNumber())
                .mobileNumber(dto.mobileNumber())
                .dateOfBirth(dto.dateOfBirth())
                .build();
    }

    public void updateFromWriteDto(IdentityDetailsWriteDto dto, NameAddress entity) {
        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setTitle(dto.title());
        entity.setForename1(dto.forename1());
        entity.setForename2(dto.forename2());
        entity.setForename3(dto.forename3());
        entity.setSurname(dto.surname());
        entity.setAddress1(dto.addressLine1());
        entity.setAddress2(dto.addressLine2());
        entity.setAddress3(dto.addressLine3());
        entity.setAddress4(dto.addressLine4());
        entity.setAddress5(dto.addressLine5());
        entity.setPostcode(dto.postcode());
        entity.setEmailAddress(dto.emailAddress());
        entity.setTelephoneNumber(dto.telephoneNumber());
        entity.setMobileNumber(dto.mobileNumber());
        entity.setDateOfBirth(dto.dateOfBirth());
    }
}
