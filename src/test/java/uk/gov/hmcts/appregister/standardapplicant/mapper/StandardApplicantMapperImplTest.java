package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.model.IndividualOrOrganisation;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;

class StandardApplicantMapperImplTest {
    @Test
    void testToReadGetSummaryDtoWithOrganisationName() {
        StandardApplicant testData = new StandardApplicantTestData().someComplete();
        StandardApplicantMapperImpl mapper = new StandardApplicantMapperImpl();
        StandardApplicantGetSummaryDto standardApplicantDto = mapper.toReadGetSummaryDto(testData);

        Assertions.assertEquals(testData.getAddressLine1(), standardApplicantDto.getAddressLine1());
        Assertions.assertEquals(testData.getName(), standardApplicantDto.getName());
        Assertions.assertEquals(testData.getApplicantCode(), standardApplicantDto.getCode());
        Assertions.assertEquals(
                testData.getApplicantStartDate(), standardApplicantDto.getStartDate());
        Assertions.assertEquals(
                testData.getApplicantEndDate(), standardApplicantDto.getEndDate().get());
    }

    @Test
    void testToReadGetSummaryDtoWithIndividualName() {
        StandardApplicant testData = new StandardApplicantTestData().someComplete();
        testData.setName(null);
        StandardApplicantMapperImpl mapper = new StandardApplicantMapperImpl();
        StandardApplicantGetSummaryDto standardApplicantDto = mapper.toReadGetSummaryDto(testData);

        Assertions.assertEquals(testData.getAddressLine1(), standardApplicantDto.getAddressLine1());
        Assertions.assertEquals(
                IndividualOrOrganisation.getIndividualNameString(
                        testData.getApplicantForename1(), testData.getApplicantSurname()),
                standardApplicantDto.getName());
        Assertions.assertEquals(testData.getApplicantCode(), standardApplicantDto.getCode());
        Assertions.assertEquals(
                testData.getApplicantStartDate(), standardApplicantDto.getStartDate());
        Assertions.assertEquals(
                testData.getApplicantEndDate(), standardApplicantDto.getEndDate().get());
    }
}
