package uk.gov.hmcts.appregister.common.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provides a fixed local identity when the {@code nosecurity} profile is active.
 */
@Component
@Profile("nosecurity")
public class NoSecurityUserProvider extends UserProvider {

    private final String userId;
    private final String email;
    private final String[] roles;

    public NoSecurityUserProvider(
            @Value("${app.security.nosecurity.user-id:local:nosecurity}") String userId,
            @Value("${app.security.nosecurity.email:nosecurity@appreg.local}") String email,
            @Value("${app.security.nosecurity.roles:LOCAL_NO_SECURITY}") String[] roles) {
        this.userId = userId;
        this.email = email;
        this.roles = Arrays.stream(roles).filter(StringUtils::hasText).toArray(String[]::new);
    }

    @Override
    public String[] getRoles() {
        return roles.clone();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getEmail() {
        return email;
    }
}
