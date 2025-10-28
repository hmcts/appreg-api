package uk.gov.hmcts.appregister.audit.listener.diff;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class Difference {
    private final String tableName;
    private final String fieldName;
    private final String oldValue;
    private final String newValue;
}
