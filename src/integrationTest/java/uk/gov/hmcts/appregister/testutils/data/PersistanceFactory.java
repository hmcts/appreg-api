package uk.gov.hmcts.appregister.testutils.data;

public class PersistanceFactory {
    static AppCodeTestData appCodeTestData = new AppCodeTestData();

    public static AppCodeTestData getAppCodeTestData() {
        return appCodeTestData;
    }
}
