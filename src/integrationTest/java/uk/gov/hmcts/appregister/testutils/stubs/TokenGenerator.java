package uk.gov.hmcts.appregister.testutils.stubs;

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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;

@Builder
@Slf4j
@Getter
public class TokenGenerator {

    public static final String DEFAULT_AUDIENCE = "audience";
    public static final String DEFAULT_ISSUER = "issuer";
    public static final String DEFAULT_USERNAME = "app.registry@hmcts.net";

    @Builder.Default private String issuer = DEFAULT_ISSUER;

    @Builder.Default private String audience = DEFAULT_AUDIENCE;

    @Builder.Default private String email = DEFAULT_USERNAME;

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
     * builds an official jwt token that can be used to test the darts api end to end.
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
                        .claim("emails", List.of(email))
                        .claim("sub", email)
                        .claim(
                                "roles",
                                StringUtils.join(
                                        roles.stream()
                                                .map(RoleEnum::getRole)
                                                .toArray(String[]::new),
                                        ","))
                        .claim("aud", audience)
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

    public String getGlobalKey() {
        return globalKey.toPublicJWK().toJSONString();
    }
}
