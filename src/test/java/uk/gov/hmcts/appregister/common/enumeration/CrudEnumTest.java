package uk.gov.hmcts.appregister.common.enumeration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CrudEnumTest {
    @Test
    void testFromValue() {
        Assertions.assertEquals(CrudEnum.DELETE, CrudEnum.fromValue(CrudEnum.DELETE.getValue()));
    }

    @Test
    void testFromValueFail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CrudEnum.fromValue('Z'));
    }
}
