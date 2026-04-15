package uk.gov.hmcts.appregister.applicationlist.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;

@Component
@ConfigurationProperties(prefix = "application-list.sort")
@Getter
@Setter
@Slf4j
public class ApplicationListSortProperties {

    private List<String> disabledSortKeys = new ArrayList<>();

    public Set<ApplicationListSortFieldEnum> getDisabledEnums() {
        return disabledSortKeys.stream()
                .map(ApplicationListSortFieldEnum::fromApiValue)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    @PostConstruct
    void validate() {
        for (String key : disabledSortKeys) {
            if (ApplicationListSortFieldEnum.fromApiValue(key).isEmpty()) {
                log.warn("Unknown disabled sort key '{}' - ignoring", key);
            }
        }
    }
}
