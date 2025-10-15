package uk.gov.hmcts.appregister.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

public class EtagUtil {

    /**
     * generates an ETag for a given entity based on its ID, version, and class name.
     *
     * @return The generated ETag string.
     */
    public static String generateEtag(UUID id, Versionable versionable) {
        try {
            String base =
                    id
                            + ":"
                            + versionable.getVersion()
                            + ":"
                            + versionable.getClass().getCanonicalName();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest) + "\""; // quotes required
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ETag", e);
        }
    }
}
