package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;

public class StandardApplicantMapperTest {
    @Test
    public void testStandardApplicantMapperForIndividual() {
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        // make the name null to simulate individual
        standardApplicant.setName(null);

        StandardApplicantMapper standardApplicantMapper = new StandardApplicantMapperImpl();
        StandardApplicantGetDetailDto standardApplicantGetDetailDto =
                standardApplicantMapper.toReadGetDto(standardApplicant);

        Assertions.assertEquals(
                standardApplicant.getApplicantStartDate(),
                standardApplicantGetDetailDto.getStartDate());
        Assertions.assertTrue(standardApplicantGetDetailDto.getEndDate().isPresent());
        Assertions.assertEquals(
                standardApplicant.getApplicantEndDate(),
                standardApplicantGetDetailDto.getEndDate().get());
        Assertions.assertNotNull(standardApplicantGetDetailDto.getApplicant());
        Assertions.assertNotNull(standardApplicantGetDetailDto.getApplicant().getPerson());
        Assertions.assertNull(standardApplicantGetDetailDto.getApplicant().getOrganisation());
        Assertions.assertNotNull(
                standardApplicantGetDetailDto.getApplicant().getPerson().getName());
        Assertions.assertNotNull(
                standardApplicant.getApplicantTitle(),
                standardApplicantGetDetailDto.getApplicant().getPerson().getName().getTitle());
        Assertions.assertNotNull(
                standardApplicant.getApplicantForename1(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertNotNull(
                standardApplicant.getApplicantForename2(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getSecondForename());
        Assertions.assertNotNull(
                standardApplicant.getApplicantForename3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getThirdForename());
        Assertions.assertNotNull(
                standardApplicant.getApplicantSurname(),
                standardApplicantGetDetailDto.getApplicant().getPerson().getName().getSurname());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine1(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine1());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine2(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine2());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine3());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine5(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine5());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine4(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine4());
        Assertions.assertNotNull(
                standardApplicant.getEmailAddress(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getEmail());
        Assertions.assertNotNull(
                standardApplicant.getMobileNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getMobile());
        Assertions.assertNotNull(
                standardApplicant.getTelephoneNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getPhone());
        Assertions.assertNotNull(
                standardApplicant.getPostcode(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getPostcode());
    }

    @Test
    public void testStandardApplicantMapperForOrganisation() {
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();

        StandardApplicantMapper standardApplicantMapper = new StandardApplicantMapperImpl();
        StandardApplicantGetDetailDto standardApplicantGetDetailDto =
                standardApplicantMapper.toReadGetDto(standardApplicant);

        Assertions.assertEquals(
                standardApplicant.getApplicantStartDate(),
                standardApplicantGetDetailDto.getStartDate());
        Assertions.assertTrue(standardApplicantGetDetailDto.getEndDate().isPresent());
        Assertions.assertEquals(
                standardApplicant.getApplicantEndDate(),
                standardApplicantGetDetailDto.getEndDate().get());
        Assertions.assertNotNull(standardApplicantGetDetailDto.getApplicant());
        Assertions.assertNotNull(standardApplicantGetDetailDto.getApplicant().getOrganisation());

        Assertions.assertEquals(
                standardApplicant.getName(),
                standardApplicantGetDetailDto.getApplicant().getOrganisation().getName());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine1(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine1());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine2(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine2());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine3());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine5(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine5());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine4(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine4());
        Assertions.assertNotNull(
                standardApplicant.getEmailAddress(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getEmail());
        Assertions.assertNotNull(
                standardApplicant.getMobileNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getMobile());
        Assertions.assertNotNull(
                standardApplicant.getTelephoneNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getPhone());
        Assertions.assertNotNull(
                standardApplicant.getPostcode(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getPostcode());
    }
}
