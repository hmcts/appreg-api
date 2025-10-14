package uk.gov.hmcts.appregister.applicationentry.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.dto.IdentityDetailsDto;
import uk.gov.hmcts.appregister.applicationentry.dto.IdentityDetailsWriteDto;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

/**
 * Mapper for converting between NameAddress entity and its DTOs.
 */
/**
 * Mapper for converting between NameAddress entity and its DTOs.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class NameAddressMapper {
    @Mapping(target = "addressLine1", source = "address1")
    @Mapping(target = "addressLine2", source = "address2")
    @Mapping(target = "addressLine3", source = "address3")
    @Mapping(target = "addressLine4", source = "address4")
    @Mapping(target = "addressLine5", source = "address5")
    abstract IdentityDetailsDto toReadDto(NameAddress entity);

    @Mapping(target = "address1", source = "addressLine1")
    @Mapping(target = "address2", source = "addressLine2")
    @Mapping(target = "address3", source = "addressLine3")
    @Mapping(target = "address4", source = "addressLine4")
    @Mapping(target = "address5", source = "addressLine5")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    abstract NameAddress createFromWriteDto(IdentityDetailsWriteDto dto);

    void updateFromWriteDto(IdentityDetailsWriteDto dto, NameAddress entity) {
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
