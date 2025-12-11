package uk.gov.hmcts.appregister.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

public class EtagUtil {

    /**
     * generates an ETag for a given entity based on its ID, version, and class name.
     *
     * @param id The list of Keyable entities to generate the ETag for.
     * @return The generated ETag string.
     */
    public static String generateEtag(List<Keyable> id) {
        try {
            String base = getStringRepresentation(id);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest) + "\""; // quotes required
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ETag", e);
        }
    }

    /**
     * gets a unique string representation of a list of Keyable entities.
     *
     * @param ids The list of Keyable entities
     * @return The string that is unique to the configuration
     */
    private static String getStringRepresentation(List<Keyable> ids) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Keyable keyable : ids) {
            stringBuilder
                .append(keyable.getId().toString())
                .append(":")
                .append(
                    (keyable instanceof Versionable)
                        ? ((Versionable) keyable).getVersion().toString()
                        : "")
                .append(keyable.getClass().getCanonicalName());
        }

        return stringBuilder.toString();
    }
}
