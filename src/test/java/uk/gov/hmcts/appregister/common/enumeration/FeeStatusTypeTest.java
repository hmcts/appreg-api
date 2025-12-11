package uk.gov.hmcts.appregister.common.enumeration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FeeStatusTypeTest {

    @Test
    void testFromDisplayName() {
        Assertions.assertEquals(
                FeeStatusType.DUE,
                FeeStatusType.fromDisplayName(FeeStatusType.DUE.getDisplayName()));
    }

    @Test
    void testFromDisplayNameFail() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FeeStatusType.fromDisplayName("Non Existing"));
    }
}
