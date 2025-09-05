package uk.gov.hmcts.appregister.common.entity.security;

import com.nimbusds.jwt.JWTClaimNames;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Represents the logged in user
 *
 * <p>This class can be injecting anywhere that needs to find out the current user and their roles.
 * This class assumes that we authenticated using a JWT token and that the token contains a suitable
 * claim
 *
 * <p>This class is simply a parser it does not validate the integrity of the token this is assumed
 * to have already taken place
 */
@Component
public class AuthenticatedUser {

    private static final String ROLES_CLAIM = "roles";

    public String[] getRoles() {
        if (getJwt() == null) {
            return new String[0];
        }

        return getJwt().getClaimAsStringList(ROLES_CLAIM).toArray(new String[0]);
    }

    /**
     * Gets the user from the subject claim.
     *
     * @return The user
     */
    public String getUser() {
        if (getJwt() == null) {
            return "unknown";
        }
        return getJwt().getClaimAsString(JWTClaimNames.SUBJECT);
    }

    /**
     * Returns a number that uniquely identifies the user.
     *
     * @return The user number
     */
    // TODO: We need a number to insert into the database. Not sure where we get the number from
    public Long getUserNumber() {
        if (getJwt() == null) {
            return 0L;
        }
        // TODO: What is the users number
        return 0L;
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
