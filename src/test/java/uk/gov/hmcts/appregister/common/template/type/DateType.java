package uk.gov.hmcts.appregister.common.template.type;

public class DateType implements DataType {
    @Override
    public boolean validateForType(String value) {
        return true;
    }
}
