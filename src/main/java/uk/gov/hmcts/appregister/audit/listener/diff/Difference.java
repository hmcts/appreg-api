package uk.gov.hmcts.appregister.audit.listener.diff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Difference {
    private final String tableName;
    private final String fieldName;
    private final String oldValue;
    private final String newValue;
}
