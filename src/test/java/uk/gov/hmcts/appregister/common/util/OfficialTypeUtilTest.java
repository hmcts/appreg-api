package uk.gov.hmcts.appregister.common.util;

import static uk.gov.hmcts.appregister.common.util.OfficialTypeUtil.CLERK_CODE;
import static uk.gov.hmcts.appregister.common.util.OfficialTypeUtil.MAGISTRATE_CODE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;

class OfficialTypeUtilTest {

    @Test
    void testFromCodeWithMagistrateCode() {
        OfficialType officialType = OfficialTypeUtil.fromCode(MAGISTRATE_CODE);
        Assertions.assertEquals(OfficialType.MAGISTRATE, officialType);
    }

    @Test
    void testFromCodeWithClerkCode() {
        OfficialType officialType = OfficialTypeUtil.fromCode(CLERK_CODE);
        Assertions.assertEquals(OfficialType.CLERK, officialType);
    }

    @Test
    void testFromCodeWithNullCodeDefaultsToMagistrateCode() {
        OfficialType officialType = OfficialTypeUtil.fromCode(null);
        Assertions.assertEquals(OfficialType.MAGISTRATE, officialType);
    }

    @Test
    void testFromCodeWithInvalidCodeDefaultsToMagistrateCode() {
        OfficialType officialType = OfficialTypeUtil.fromCode("A");
        Assertions.assertEquals(OfficialType.MAGISTRATE, officialType);
    }
}
