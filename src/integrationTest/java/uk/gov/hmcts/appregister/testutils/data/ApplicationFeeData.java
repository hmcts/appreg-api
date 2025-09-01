package uk.gov.hmcts.appregister.testutils.data;

import uk.gov.hmcts.appregister.applicationfee.model.ApplicationFee;

public class ApplicationFeeData implements Persistable<ApplicationFee.ApplicationFeeBuilder> {
    @Override
    public ApplicationFee.ApplicationFeeBuilder someMinimal() {
        ApplicationFee.ApplicationFeeBuilder data = ApplicationFee.builder();
        return data;
    }
}
