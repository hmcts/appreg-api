package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;

public class StandardApplicantMapperTest {
    @Test
    public void testStandardApplicantMapperForIndividual() {
        val standardApplicant = new StandardApplicantTestData().someComplete();

        // make the name null to simulate individual
        standardApplicant.setName(null);

        val standardApplicantMapper = new StandardApplicantMapperImpl();
        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());
        val standardApplicantGetDetailDto = standardApplicantMapper.toReadGetDto(standardApplicant);

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
                        .getSecondForename()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getApplicantForename3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getThirdForename()
                        .get());
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
                        .getAddressLine2()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine3()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine5(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine5()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine4(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getAddressLine4()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getEmailAddress(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getEmail()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getMobileNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getMobile()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getTelephoneNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getContactDetails()
                        .getPhone()
                        .get());
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
        val standardApplicant = new StandardApplicantTestData().someComplete();

        val standardApplicantMapper = new StandardApplicantMapperImpl();
        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());

        val standardApplicantGetDetailDto = standardApplicantMapper.toReadGetDto(standardApplicant);

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
                        .getAddressLine2()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine3(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine3()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine5(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine5()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getAddressLine4(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine4()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getEmailAddress(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getEmail()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getMobileNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getMobile()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getTelephoneNumber(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getPhone()
                        .get());
        Assertions.assertNotNull(
                standardApplicant.getPostcode(),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getPostcode());
    }

    @Test
    void testNoEntity() {
        val record = new CodeAndName(null, null, null, null, null);

        var mapper = new StandardApplicantMapperImpl();
        Assertions.assertNotNull(mapper.toEntity(record));
    }

    @Test
    void testSearchAuditEntityIncludesAllAuditedFilters() {
        // Build the same lightweight surrogate entity that the GET /standard-applicants search
        // endpoint passes into the audit framework.
        val record =
                new CodeAndName(
                        "APP001",
                        "John Doe",
                        "123 High Street",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 12, 31));

        var mapper = new StandardApplicantMapperImpl();
        val entity = mapper.toEntity(record);

        // Each populated field below maps to a real database column and is now eligible for READ
        // audit extraction.
        Assertions.assertEquals("APP001", entity.getApplicantCode());
        Assertions.assertEquals("John Doe", entity.getName());
        Assertions.assertEquals("123 High Street", entity.getAddressLine1());
        Assertions.assertEquals(LocalDate.of(2026, 4, 1), entity.getApplicantStartDate());
        Assertions.assertEquals(LocalDate.of(2026, 12, 31), entity.getApplicantEndDate());
    }
}
