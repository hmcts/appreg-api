package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/** Mapper for StandardApplicant entity to StandardApplicantDto. */
@Component
public class StandardApplicantMapper {

  public StandardApplicantDto toReadDto(StandardApplicant entity) {
    if (entity == null) {
      return null;
    }

    return new StandardApplicantDto(
        entity.getId(),
        entity.getApplicantCode(),
        entity.getApplicantTitle(),
        entity.getName(),
        entity.getApplicantForename1(),
        entity.getApplicantForename2(),
        entity.getApplicantForename3(),
        entity.getApplicantSurname(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getAddressLine3(),
        entity.getAddressLine4(),
        entity.getAddressLine5(),
        entity.getPostcode(),
        entity.getEmailAddress(),
        entity.getTelephoneNumber(),
        entity.getMobileNumber(),
        entity.getApplicantStartDate(),
        entity.getApplicantEndDate());
  }
}
