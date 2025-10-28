package uk.gov.hmcts.appregister.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.util.ReflectionCaches;

import java.util.List;

public class ReflectionsCachesTest {
    @Test
    public void testLoadCachesIsLoading() {
        ReflectionCaches.ReflectionMeta data = ReflectionCaches.METHOD_CACHE.get(ApplicationCode.class);
        ReflectionCaches.ReflectionMeta data2 = ReflectionCaches.METHOD_CACHE.get(ApplicationCode.class);

        // assert the same reference
        Assertions.assertTrue(data == data2);

        ReflectionCaches.ReflectionMeta appLstData = ReflectionCaches.METHOD_CACHE.get(ApplicationList.class);
        ReflectionCaches.ReflectionMeta appLstData2 = ReflectionCaches.METHOD_CACHE.get(ApplicationList.class);

        // assert the same reference
        Assertions.assertTrue(appLstData.methods().size() != 0);
        Assertions.assertTrue(appLstData.methods() == appLstData2.methods());
    }
}
