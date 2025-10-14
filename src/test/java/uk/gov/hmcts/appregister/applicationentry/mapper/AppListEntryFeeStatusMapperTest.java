package uk.gov.hmcts.appregister.applicationentry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.dto.AppListEntryFeeStatusDto;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.data.AppListEntryFeeStatusTestData;

public class AppListEntryFeeStatusMapperTest {

    @Test
    public void toReadDto() {
        AppListEntryFeeStatusTestData testData = new AppListEntryFeeStatusTestData();
        AppListEntryFeeStatusMapperImpl mapper = new AppListEntryFeeStatusMapperImpl();
        AppListEntryFeeStatus status = testData.someComplete();

        AppListEntryFeeStatusDto dto = mapper.toReadDto(status);
        Assertions.assertEquals(
                status.getAppListEntry().getEntryFeeIds().getFirst().getFeeId().getDescription(),
                dto.feeDescription());
        Assertions.assertEquals(FeeStatusType.DUE, dto.feeStatus());
        Assertions.assertEquals(
                status.getAppListEntry().getEntryFeeIds().getFirst().getFeeId().getAmount(),
                dto.amount());
        Assertions.assertEquals(status.getAlefsPaymentReference(), dto.paymentReference());
        Assertions.assertEquals(status.getAlefsFeeStatusDate(), dto.statusDate());
    }
}
