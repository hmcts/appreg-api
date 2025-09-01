package uk.gov.hmcts.appregister.testutils.data;

import uk.gov.hmcts.appregister.applicationcode.model.ApplicationCode;

public class AppCodeTestData implements Persistable <ApplicationCode.ApplicationCodeBuilder> {
    @Override
    public ApplicationCode.ApplicationCodeBuilder someMinimal() {
        ApplicationCode.ApplicationCodeBuilder data = ApplicationCode.builder();
        return data;
    }
}
