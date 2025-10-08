package uk.gov.hmcts.appregister.testutils.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;

@Builder
@Slf4j
@Getter
public class TokenGenerator {

    public static final String DEFAULT_AUDIENCE = "audience";
    public static final String DEFAULT_ISSUER = "issuer";
    public static final String DEFAULT_USERNAME = "app.registry@hmcts.net";
    public static final String DEFAULT_TID = "00000000-0000-0000-0000-000000000000";
    public static final String DEFAULT_OID = "11111111-1111-1111-1111-111111111111";

    @Builder.Default private String issuer = DEFAULT_ISSUER;

    @Builder.Default private String audience = DEFAULT_AUDIENCE;

    @Builder.Default private String email = DEFAULT_USERNAME;

    @Builder.Default private String tid = DEFAULT_TID;

    @Builder.Default private String oid = DEFAULT_OID;

    @Builder.Default private Date expiredDate = Date.from(Instant.now().plusSeconds(SECONDS));

    @Builder.Default private List<RoleEnum> roles = List.of();

    private boolean useGlobalKey;

    private boolean invalidToken;

    private static final int SECONDS = 216_000;

    private static RSAKey globalKey;

    static {
        try {
            globalKey =
                    new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                            .keyUse(KeyUse.SIGNATURE)
                            .keyID("1234")
                            .generate();
        } catch (JOSEException joseException) {
            log.error("Error creating global key" + joseException);
        }
    }

    public TokenAndJwksKey fetchTokenForRole() throws JOSEException {
        if (!invalidToken) {
            return fetchToken(globalKey);
        } else {
            return fetchInvalidSignatureToken();
        }
    }

    public TokenAndJwksKey fetchInvalidSignatureToken() throws JOSEException {
        RSAKey rsaKey =
                new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID("12344343")
                        .generate();

        return fetchToken(rsaKey);
    }

    /**
     * builds an official jwt token that can be used to test the app register api end to end.
     *
     * @param rsaKey The rsa key to sign the token with
     * @return The token as well as the jwks key payload to validate the token.
     * @throws com.nimbusds.jose.JOSEException Any problems fetching a token.
     */
    private TokenAndJwksKey fetchToken(RSAKey rsaKey) throws JOSEException {
        if (issuer == null || email == null || audience == null) {
            throw new IllegalArgumentException("Required inputs not supplied");
        }
        RSAKey key = rsaKey;
        JWSHeader header =
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(key.getKeyID())
                        .build();

        // setup our claims
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(audience)
                        .expirationTime(expiredDate)
                        .claim(StandardClaimNames.EMAIL, List.of(email))
                        .claim(StandardClaimNames.SUB, email)
                        .claim(
                                org.springframework.security.oauth2.core.oidc.StandardClaimNames
                                        .PREFERRED_USERNAME,
                                email)
                        .claim("tid", tid)
                        .claim("oid", oid)
                        .claim(
                                "roles",
                                StringUtils.join(
                                        roles.stream()
                                                .map(RoleEnum::getRole)
                                                .toArray(String[]::new),
                                        ","))
                        .build();

        // create a signed token using the private key
        SignedJWT signedJwt = new SignedJWT(header, claimsSet);
        signedJwt.sign(new RSASSASigner(key.toRSAPrivateKey()));

        // return the token and the jwks validating public key for this token
        TokenAndJwksKey token = new TokenAndJwksKey();
        token.setToken(signedJwt.serialize());
        token.setJwksKey(key.toPublicJWK().toJSONString());

        return token;
    }

    /**
     * A convenience method to return a spring jwt object for the token generated.
     *
     * @return The jwt token
     */
    public Jwt getJwtFromToken() throws JOSEException, ParseException {
        com.nimbusds.jwt.SignedJWT signedJwt =
                com.nimbusds.jwt.SignedJWT.parse(fetchTokenForRole().getToken());
        return new Jwt(
                fetchTokenForRole().getToken(),
                signedJwt.getJWTClaimsSet().getIssueTime() == null
                        ? Instant.now()
                        : signedJwt.getJWTClaimsSet().getIssueTime().toInstant(),
                signedJwt.getJWTClaimsSet().getExpirationTime().toInstant(),
                signedJwt.getHeader().toJSONObject(),
                signedJwt.getJWTClaimsSet().getClaims());
    }

    public String getGlobalKey() {
        return globalKey.toPublicJWK().toJSONString();
    }
}
