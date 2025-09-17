package uk.gov.hmcts.appregister.applicationentry.util;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic parser interface for parsing files into a list of objects.
 *
 * @param <T> the type of objects to be parsed from the file
 */
public interface Parser<T> {
    List<T> parse(MultipartFile file);
}
