package uk.gov.hmcts.appregister.common.template.type;

public class TextDataType implements DataType {
    @Override
    public boolean validateForType(String value) {
        return value != null
                && value.indexOf('{') < 0
                && value.indexOf('}') < 0
                && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0;
    }
}
