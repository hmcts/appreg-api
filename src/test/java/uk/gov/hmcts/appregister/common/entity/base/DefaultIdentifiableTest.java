package uk.gov.hmcts.appregister.common.entity.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultIdentifiableTest {

    @Test
    void testDefaultMethods() {
        Identifiable identifiable = new Identifiable() {};
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getCode());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getTitle());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getName());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getDescription());
    }
}
