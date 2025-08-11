package uk.gov.hmcts.appregister.applicationentry.util;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface Parser<T> {
    List<T> parse(MultipartFile file);
}
