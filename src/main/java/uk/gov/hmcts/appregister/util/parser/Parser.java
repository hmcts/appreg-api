package uk.gov.hmcts.appregister.util.parser;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface Parser<T> {
    List<T> parse(MultipartFile file);
}
