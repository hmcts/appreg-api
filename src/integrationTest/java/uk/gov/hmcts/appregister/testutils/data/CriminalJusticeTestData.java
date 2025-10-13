package uk.gov.hmcts.appregister.testutils.data;

import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.testutils.StringUtil;

public class CriminalJusticeTestData
        implements Persistable<
                CriminalJusticeArea, CriminalJusticeArea.CriminalJusticeAreaBuilder> {
    @Override
    public CriminalJusticeArea.CriminalJusticeAreaBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        CriminalJusticeArea.CriminalJusticeAreaBuilder data = CriminalJusticeArea.builder();
        data.code(StringUtil.stripToMax(uniqueId.toString(), 2))
                .description(StringUtil.stripToMax("description" + uniqueId, 35))
                .build();

        return data;
    }

    @Override
    public CriminalJusticeArea.CriminalJusticeAreaBuilder someMaximal() {
        return Persistable.super.someMaximal();
    }
}
