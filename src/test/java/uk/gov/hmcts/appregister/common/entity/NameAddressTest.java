package uk.gov.hmcts.appregister.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;

class NameAddressTest {

    @Test
    void legacyNameGetters_splitMiddleNameAndHandleNull() {
        var entity = new NameAddress();

        assertNull(entity.getForename2());
        assertNull(entity.getForename3());

        entity.setMiddleName("Anne Louise");

        assertEquals("Anne", entity.getForename2());
        assertEquals("Louise", entity.getForename3());
    }

    @Test
    void setForename2_and_setForename3_rebuildMiddleName() {
        var entity = new NameAddress();

        entity.setForename2("Anne");
        assertEquals("Anne", entity.getMiddleName());

        entity.setForename3("Louise");
        assertEquals("Anne Louise", entity.getMiddleName());

        entity.setForename2(null);
        assertEquals("Louise", entity.getMiddleName());

        entity.setForename3(null);
        assertEquals("Louise", entity.getMiddleName());

        entity.setMiddleName(null);
        entity.setForename3(null);
        assertNull(entity.getMiddleName());
    }

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
