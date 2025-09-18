package uk.gov.hmcts.appregister.testutils.token;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenAndJwksKey {
    private String token;
    private String jwksKey;
}
