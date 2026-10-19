package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.UUID;
import org.instancio.Instancio;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

public class StandardApplicantTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                StandardApplicant, StandardApplicant.StandardApplicantBuilder> {

    @Override
    public StandardApplicant someComplete() {
        StandardApplicant applicant =
                Instancio.of(StandardApplicant.class)
                        .ignore(field(StandardApplicant::getId))
                        .ignore(field(StandardApplicant::getVersion))
                        .create();

        applicant.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        applicant.setPostcode("AB12CD");
        applicant.setTelephoneNumber("01234567");
        applicant.setMobileNumber("07123456789");
        applicant.setApplicantCode("APPCODE");
        return applicant;
    }
}
