package uk.gov.hmcts.appregister.applicationlist.mapper;

import static uk.gov.hmcts.appregister.util.ApplicationListEntryOfficialPrintProjectionUtil.applicationListEntryOfficialPrintProjection;
import static uk.gov.hmcts.appregister.util.TestConstants.MR;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_SURNAME;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

class ApplicationListOfficialMapperTest {

    @Test
    void testToOfficialDto_provideValidData_validDtoGenerated() {
        var projection =
                applicationListEntryOfficialPrintProjection()
                        .type(uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE)
                        .title(MR)
                        .forename(PERSON1_FORENAME1)
                        .surname(PERSON1_SURNAME)
                        .build();

        var mapper = new ApplicationListOfficialMapperImpl();
        var dto = mapper.toOfficialDto(projection);

        Assertions.assertEquals(OfficialType.MAGISTRATE, dto.getType());
        Assertions.assertEquals(MR, dto.getTitle());
        Assertions.assertEquals(PERSON1_FORENAME1, dto.getForename());
        Assertions.assertEquals(PERSON1_SURNAME, dto.getSurname());
    }
}
