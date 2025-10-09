package uk.gov.hmcts.appregister.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.JwtError;

/**
 * Represents the logged-in user in the form of JWT token.
 *
 * <p>This class can be injected anywhere that needs to find out the current user and their roles.
 * This class assumes that we authenticated using a JWT token and that the token contains a suitable
 * claim
 *
 * <p>This class is simply a parser it does not validate the integrity of the token this is assumed
 * to have already taken place
 */
@Component
@SuppressWarnings({"java:S1135", "java:S3516"})
public class UserProvider {

    private static final String ROLES_CLAIM = "roles";
    private static final String TENET_ID_CLAIM = "tid";
    private static final String OBJECT_ID_CLAIM = "oid";
    private static final String EMAIL_CLAIM = "preferred_username";

    public String[] getRoles() {
        if (getJwt() == null) {
            return new String[0];
        }

        return getJwt().getClaimAsStringList(ROLES_CLAIM).toArray(new String[0]);
    }

    public String getUserId() {
        Jwt jwt = getJwt();
        assert jwt != null;
        String tid = jwt.getClaimAsString(TENET_ID_CLAIM);
        String oid = jwt.getClaimAsString(OBJECT_ID_CLAIM);

        if (tid == null || oid == null) {
            throw new AppRegistryException(
                    JwtError.INVALID_TOKEN, "The token was malformed or invalid");
        }

        return tid + ":" + oid;
    }

    public String getEmail() {
        Jwt jwt = getJwt();
        assert jwt != null;
        String email = jwt.getClaimAsString(EMAIL_CLAIM);

        if (email == null) {
            throw new AppRegistryException(
                    JwtError.INVALID_TOKEN, "The token was malformed or invalid");
        }

        return email;
    }

    /**
     * gets the token from thread local using {@link} SecurityContextHolder}.
     *
     * @return The jwt or null if not found
     */
    private Jwt getJwt() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                        instanceof Jwt jwt) {
            return jwt;
        }

        return null;
    }
}
