package uk.gov.hmcts.appregister.common.template.wording;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.template.type.DataType;
import uk.gov.hmcts.appregister.common.template.type.TextDataType;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;

/**
 * A wording template mapping data types to their respective classes.
 */
@Getter
public enum WordingDataTypes {
    TEXT(TemplateConstraint.TypeEnum.TEXT, new TextDataType());

    private final TemplateConstraint.TypeEnum value;
    private final DataType type;

    WordingDataTypes(TemplateConstraint.TypeEnum value, DataType type) {
        this.value = value;
        this.type = type;
    }
}
