package uk.gov.hmcts.appregister.common.template.wording;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.template.type.DataType;
import uk.gov.hmcts.appregister.common.template.type.DateType;
import uk.gov.hmcts.appregister.common.template.type.TextDataType;

/**
 * A wording template mapping data types to their respective classes.
 */
@Getter
public enum WordingDataTypes {
    TEXT("TEXT", new TextDataType()),
    DATE("DATE", new DateType()),;

    private final String value;
    private final DataType type;

    WordingDataTypes(String value, DataType type) {
        this.value = value;
        this.type = type;
    }
}
