package uk.gov.hmcts.appregister.testutils.data;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.testutils.StringUtil;

import static uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper.TRUE_VALUE;

public class ApplicationCodeTestData
        implements Persistable<ApplicationCode.ApplicationCodeBuilder> {
    @Override
    public ApplicationCode.ApplicationCodeBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationCode.ApplicationCodeBuilder data = ApplicationCode.builder();
        data.applicationCode(StringUtil.stripToMax(uniqueId.toString(), 10))
                .title("title" + uniqueId)
                .wording("wording" + uniqueId)
                .feeDue("1")
                .requiresRespondent(TRUE_VALUE)
                .startDate(OffsetDateTime.now(ZoneOffset.UTC))
                .bulkRespondentAllowed(TRUE_VALUE)
                .build();

        return data;
    }

    @Override
    public ApplicationCode.ApplicationCodeBuilder someMaximal() {
        return Persistable.super.someMaximal();
    }
}
