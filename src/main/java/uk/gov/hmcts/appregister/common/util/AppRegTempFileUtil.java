package uk.gov.hmcts.appregister.common.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * A utility that allows us to generate and search for temp files that may have been created across
 * the system.
 */
public class AppRegTempFileUtil {

    public static final String TEMP_FILE_EXTENSION = "appregtmp";

    private AppRegTempFileUtil() {
        // Utility class
    }

    /**
     * generates a temp file.
     *
     * @return The temp file
     */
    public static File generateTempFile() throws IOException {
        return generateTempFile("appreg");
    }

    /**
     * generates a temp file with a source-specific prefix.
     *
     * @param prefix The prefix to use in the generated file name
     * @return The temp file
     */
    public static File generateTempFile(String prefix) throws IOException {
        return File.createTempFile(
                prefix + "-" + UUID.randomUUID(), "." + TEMP_FILE_EXTENSION); // NOSONAR
        // - we want to use the default temp directory as this is guaranteed to be writable
        // and will be cleaned up by the system.
    }

    /**
     * Do temp files exist.
     *
     * @return true if they do, false if they don't
     */
    public static boolean doesTempFileExist() {
        File[] files = getTempFilesThatExist();

        return files != null && files.length > 0;
    }

    /**
     * Gets the temp files that exist.
     *
     * @return The temp files that exist
     */
    public static File[] getTempFilesThatExist() {
        return new File(System.getProperty("java.io.tmpdir"))
                .listFiles(file -> file.getName().endsWith(TEMP_FILE_EXTENSION));
    }
}
