package uk.gov.hmcts.appregister.testutils.stubs;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class TokenClient {
    public static final int SECONDS = 216_000;

    public static final String INVALID_ROLE = "invalid-role";
    public static final String INVALID_AUDIENCE = "invalid-audience";
    public static final String INVALID_ISSUER = "invalid-issuer";

    // invalid token types
    private static final int ROLE_TO_APPLY_COMMAND_LINE_OPTION = 0;
    private static final int INCORRECT_SIGNATURE_COMMAND_LINE_OPTION = 1;
    private static final int EXPIRED_COMMAND_LINE_OPTION = 2;
    private static final int INCORRECT_AUDIENCE_COMMAND_LINE_OPTION = 3;
    private static final int AUTHORISATION_FAILURE_COMMAND_LINE_OPTION = 4;
    private static final int INCORRECT_ISSUER_COMMAND_LINE_OPTION = 5;

    private static final TokenClient.CommandArgProcessor EXPIRED_TOKEN_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[EXPIRED_COMMAND_LINE_OPTION])) {
                    builder = builder.expiredDate(Date.from(Instant.now().minusSeconds(SECONDS)));
                }
                return builder;
            };

    private static final TokenClient.CommandArgProcessor INCORRECT_AUDIENCE_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[INCORRECT_AUDIENCE_COMMAND_LINE_OPTION])) {
                    builder = builder.audience(INVALID_AUDIENCE);
                }
                return builder;
            };

    private static final TokenClient.CommandArgProcessor AUTHORISATION_FAILURE_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[AUTHORISATION_FAILURE_COMMAND_LINE_OPTION])) {
                    builder = builder.roles(List.of(INVALID_ROLE));
                }

                return builder;
            };

    private static final TokenClient.CommandArgProcessor INCORRECT_ISSUER_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[INCORRECT_ISSUER_COMMAND_LINE_OPTION])) {
                    builder = builder.roles(List.of(INVALID_ISSUER));
                }

                return builder;
            };

    private static final TokenClient.CommandArgProcessor ROLE_APPLICATION_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[ROLE_TO_APPLY_COMMAND_LINE_OPTION])) {
                    builder = builder.roles(List.of(args[ROLE_TO_APPLY_COMMAND_LINE_OPTION]));
                }
                return builder;
            };

    private static final TokenClient.CommandArgProcessor SIGNATURE_COMMAND =
            (builder, args) -> {
                if (Boolean.parseBoolean(args[INCORRECT_SIGNATURE_COMMAND_LINE_OPTION])) {
                    return new TokenGenerator.TokenGeneratorBuilder().invalidToken(true);
                }
                return builder;
            };

    private static final TokenClient.CommandArgProcessor[] COMMAND_PROCESSOR = {
        ROLE_APPLICATION_COMMAND,
        EXPIRED_TOKEN_COMMAND,
        INCORRECT_AUDIENCE_COMMAND,
        AUTHORISATION_FAILURE_COMMAND,
        INCORRECT_ISSUER_COMMAND,
        SIGNATURE_COMMAND
    };

    /**
     * A command line tool to create local tokens in various states
     *
     * @param args command line args [0] - Role name for token [1] - Valid or not [2] - Incorrect
     *     signature [3] - Expired [4] - Incorrect audience [5] - Authorisation failure
     */
    public static void main(String args[]) throws Exception {

        // generate a token for a role as well as an invalid token
        if (args.length != 6) {
            throw new IllegalArgumentException();
        }

        TokenGenerator.TokenGeneratorBuilder builder = new TokenGenerator.TokenGeneratorBuilder();

        // process the invalid commands
        for (int i = 0; i < COMMAND_PROCESSOR.length; i++) {
            builder = COMMAND_PROCESSOR[i].process(builder, args);
        }

        System.out.println("Token : " + builder.build().fetchTokenForRole().getToken());
    }

    @FunctionalInterface
    interface CommandArgProcessor {
        TokenGenerator.TokenGeneratorBuilder process(
                TokenGenerator.TokenGeneratorBuilder builder, String[] args);
    }
}
