package uk.gov.hmcts.appregister.common.entity.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultIdentifiableTest {

    @Test
    public void testDefaultMethods() {
        Identifiable identifiable = new Identifiable() {};
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getCode());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getTitle());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getName());
        Assertions.assertEquals(Identifiable.DEFAULT_VALUE, identifiable.getDescription());
    }
}
