package uk.gov.hmcts.appregister.util.parser;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface Parser<T> {
    List<T> parse(MultipartFile file);
}
