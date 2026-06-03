package uk.gov.hmcts.appregister.common.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;

class NameAddressTest {

    @Test
    void roleHelpers_returnExpectedFlags() {
        var entity = new NameAddress();

        assertFalse(entity.isApplicant());
        assertFalse(entity.isRespondent());

        entity.setCode(NameAddressCodeType.APPLICANT);
        assertTrue(entity.isApplicant());
        assertFalse(entity.isRespondent());

        entity.setCode(NameAddressCodeType.RESPONDENT);
        assertFalse(entity.isApplicant());
        assertTrue(entity.isRespondent());
    }
}
