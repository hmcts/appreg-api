package uk.gov.hmcts.appregister.common.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

public class UserProviderTest {

    private static final String EMAIL_CLAIM = "test.user@example.com";
    private static final String TID_CLAIM = "00000000-0000-0000-0000-000000000000";
    private static final String OID_CLAIM = "11111111-1111-1111-1111-111111111111";
    private static final String ROLE_1 = "ROLE_1";
    private static final String ROLE_2 = "ROLE_2";

    private final UserProvider userProvider = new UserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRoles_returnsEmptyArray_whenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();
        String[] roles = userProvider.getRoles();
        assertNotNull(roles);
        assertEquals(0, roles.length);
    }

    @Test
    void getRoles_returnsValues_whenRolesClaimPresent() {
        setJwt(
                Map.of(
                        "roles", List.of(ROLE_1, ROLE_2),
                        "tid", TID_CLAIM,
                        "oid", OID_CLAIM,
                        "preferred_username", EMAIL_CLAIM));

        String[] roles = userProvider.getRoles();
        assertArrayEquals(new String[] {ROLE_1, ROLE_2}, roles);
    }

    @Test
    void getUserId_returnsTidColonOid_whenBothPresent() {
        setJwt(
                Map.of(
                        "tid", TID_CLAIM,
                        "oid", OID_CLAIM,
                        "preferred_username", EMAIL_CLAIM));

        assertEquals(TID_CLAIM + ":" + OID_CLAIM, userProvider.getUserId());
    }

    @Test
    void getUserId_throws_whenTidMissing() {
        setJwt(
                Map.of(
                        "oid", OID_CLAIM,
                        "preferred_username", EMAIL_CLAIM));
        AppRegistryException ex = assertThrows(AppRegistryException.class, userProvider::getUserId);
        assertTrue(
                ex.getMessage().toLowerCase().contains("tid")
                        && ex.getMessage().toLowerCase().contains("oid"));
    }

    @Test
    void getUserId_throws_whenOidMissing() {
        setJwt(
                Map.of(
                        "tid", TID_CLAIM,
                        "preferred_username", EMAIL_CLAIM));
        AppRegistryException ex = assertThrows(AppRegistryException.class, userProvider::getUserId);
        assertTrue(
                ex.getMessage().toLowerCase().contains("tid")
                        && ex.getMessage().toLowerCase().contains("oid"));
    }

    // ---------- getEmail ----------

    @Test
    void getEmail_returnsPreferredUsername_whenPresent() {
        setJwt(
                Map.of(
                        "tid", TID_CLAIM,
                        "oid", OID_CLAIM,
                        "preferred_username", EMAIL_CLAIM));
        assertEquals(EMAIL_CLAIM, userProvider.getEmail());
    }

    @Test
    void getEmail_throws_whenEmailMissing() {
        setJwt(
                Map.of(
                        "tid", TID_CLAIM,
                        "oid", OID_CLAIM));
        AppRegistryException ex = assertThrows(AppRegistryException.class, userProvider::getEmail);
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    // ---------- helpers ----------

    private static void setJwt(Map<String, Object> claims) {
        Jwt jwt =
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .claims(c -> c.putAll(claims))
                        .subject((String) claims.getOrDefault("oid", "sub"))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();

        JwtAuthenticationToken auth =
                new JwtAuthenticationToken(
                        jwt,
                        Collections.emptyList(),
                        (String) claims.getOrDefault("preferred_username", "user"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
