package uk.gov.hmcts.appregister.common.template.type;

public class TextDataType implements DataType{
    @Override
    public boolean validateForType(String value) {
        return true;
    }
}
