package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import java.time.Month;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;

class StandardApplicantMapperTest {
    @Test
    void testStandardApplicantMapperForIndividual() {
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
                standardApplicantGetDetailDto.getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "%s %s"
                        .formatted(
                                standardApplicant.getApplicantForename2(),
                                standardApplicant.getApplicantForename3()),
                standardApplicantGetDetailDto
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getMiddleName()
                        .get());
        Assertions.assertEquals(
                standardApplicant.getApplicantSurname(),
                standardApplicantGetDetailDto.getApplicant().getPerson().getName().getLastName());
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
    void testStandardApplicantMapperForOrganisation() {
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
        val codeAndName = new CodeAndName(null, null, null, null, null);

        var mapper = new StandardApplicantMapperImpl();
        Assertions.assertNotNull(mapper.toEntity(codeAndName));
    }

    @Test
    void testSearchAuditEntityIncludesAllAuditedFilters() {
        // Build the same lightweight surrogate entity that the GET /standard-applicants search
        // endpoint passes into the audit framework.
        val codeAndName =
                new CodeAndName(
                        "APP001",
                        "John Doe",
                        "123 High Street",
                        LocalDate.of(2026, Month.APRIL, 1),
                        LocalDate.of(2026, Month.DECEMBER, 31));

        var mapper = new StandardApplicantMapperImpl();
        val entity = mapper.toEntity(codeAndName);

        // Each populated field below maps to a real database column and is now eligible for READ
        // audit extraction.
        Assertions.assertEquals("APP001", entity.getApplicantCode());
        Assertions.assertEquals("John Doe", entity.getName());
        Assertions.assertEquals("123 High Street", entity.getAddressLine1());
        Assertions.assertEquals(LocalDate.of(2026, Month.APRIL, 1), entity.getApplicantStartDate());
        Assertions.assertEquals(
                LocalDate.of(2026, Month.DECEMBER, 31), entity.getApplicantEndDate());
    }

    @Test
    void testPrintRowMapsNullSourceValuesToExplicitJsonNulls() {
        val standardApplicant = new StandardApplicant();
        val mapper = new StandardApplicantMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());

        val dto =
                mapper.toPrintRowDto(
                        new StandardApplicantEnrichedProjection() {
                            @Override
                            public StandardApplicant getStandardApplicant() {
                                return standardApplicant;
                            }

                            @Override
                            public String getEffectiveName() {
                                return null;
                            }
                        });

        assertPresentNull(dto.getCode());
        assertPresentNull(dto.getUseFrom());
        assertPresentNull(dto.getName());
        assertPresentNull(dto.getUseTo());
        assertPresentNull(dto.getTitle());
        assertPresentNull(dto.getAddressLine1());
        assertPresentNull(dto.getForename1());
        assertPresentNull(dto.getAddressLine2());
        assertPresentNull(dto.getForename2());
        assertPresentNull(dto.getAddressLine3());
        assertPresentNull(dto.getForename3());
        assertPresentNull(dto.getAddressLine4());
        assertPresentNull(dto.getSurname());
        assertPresentNull(dto.getAddressLine5());
        assertPresentNull(dto.getEmailAddress());
        assertPresentNull(dto.getPostcode());
        assertPresentNull(dto.getTelephoneNumber());
        assertPresentNull(dto.getMobileNumber());
    }

    @Test
    void testStandardApplicantHydratesCanonicalMiddleNameFromLegacyForenames() {
        val standardApplicant = new StandardApplicantTestData().someComplete();
        standardApplicant.setName(null);
        standardApplicant.setApplicantForename1("Ada");
        standardApplicant.setApplicantForename2("Byron");
        standardApplicant.setApplicantForename3("King");
        standardApplicant.setApplicantSurname("Lovelace");

        val standardApplicantMapper = new StandardApplicantMapperImpl();
        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());

        val dto = standardApplicantMapper.toReadGetDto(standardApplicant);

        Assertions.assertEquals("Ada", dto.getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Byron King", dto.getApplicant().getPerson().getName().getMiddleName().get());
        Assertions.assertEquals("Lovelace", dto.getApplicant().getPerson().getName().getLastName());
    }

    private static void assertPresentNull(JsonNullable<?> value) {
        Assertions.assertTrue(value.isPresent());
        Assertions.assertNull(value.get());
    }
}
