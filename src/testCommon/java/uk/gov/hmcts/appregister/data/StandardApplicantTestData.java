package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.instancio.Instancio;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

public class StandardApplicantTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                StandardApplicant, StandardApplicant.StandardApplicantBuilder> {
    private static final AtomicInteger NEXT_CODE = new AtomicInteger();

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
        // Multiple generated applicants can share a transaction, but not a business key.
        applicant.setApplicantCode("T" + Integer.toUnsignedString(NEXT_CODE.incrementAndGet(), 36));
        return applicant;
    }
}
