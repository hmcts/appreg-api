package uk.gov.hmcts.appregister.common.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoSecurityUserProviderTest {

    @Test
    void getters_returnConfiguredValues() {
        NoSecurityUserProvider provider =
                new NoSecurityUserProvider(
                        "local:nosecurity",
                        "nosecurity@appreg.local",
                        new String[] {"LOCAL_NO_SECURITY", "Admin"});

        assertEquals("local:nosecurity", provider.getUserId());
        assertEquals("nosecurity@appreg.local", provider.getEmail());
        assertArrayEquals(new String[] {"LOCAL_NO_SECURITY", "Admin"}, provider.getRoles());
    }

    @Test
    void getRoles_returnsDefensiveCopy() {
        NoSecurityUserProvider provider =
                new NoSecurityUserProvider(
                        "local:nosecurity",
                        "nosecurity@appreg.local",
                        new String[] {"LOCAL_NO_SECURITY"});

        String[] roles = provider.getRoles();
        roles[0] = "changed";

        assertArrayEquals(new String[] {"LOCAL_NO_SECURITY"}, provider.getRoles());
    }

    @Test
    void constructor_filtersBlankRoles() {
        NoSecurityUserProvider provider =
                new NoSecurityUserProvider(
                        "local:nosecurity",
                        "nosecurity@appreg.local",
                        new String[] {"", "LOCAL_NO_SECURITY", " "});

        assertArrayEquals(new String[] {"LOCAL_NO_SECURITY"}, provider.getRoles());
    }
}
