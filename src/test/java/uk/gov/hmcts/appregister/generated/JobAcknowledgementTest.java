package uk.gov.hmcts.appregister.generated;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

class JobAcknowledgementTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(JobType.class)
    void givenUnsetTotals_whenSerialized_thenFieldsAreAbsent(JobType type) throws Exception {
        var acknowledgement =
                new JobAcknowledgement()
                        .id(UUID.randomUUID())
                        .type(type)
                        .status(JobStatus.RECEIVED);
        var json = objectMapper.readTree(objectMapper.writeValueAsString(acknowledgement));

        assertThat(json.has("mainFeeTotal")).isFalse();
        assertThat(json.has("offsiteFeeTotal")).isFalse();
        assertThat(json.has("totalFeeValue")).isFalse();
        assertThat(json.has("error_description")).isTrue();
        assertThat(json.get("error_description").isNull()).isTrue();
    }

    @Test
    void givenZeroTotals_whenSerialized_thenFieldsArePresent() throws Exception {
        var acknowledgement =
                new JobAcknowledgement()
                        .id(UUID.randomUUID())
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus.COMPLETED)
                        .mainFeeTotal(BigDecimal.ZERO)
                        .offsiteFeeTotal(BigDecimal.ZERO)
                        .totalFeeValue(BigDecimal.ZERO);
        var json = objectMapper.readTree(objectMapper.writeValueAsString(acknowledgement));

        assertThat(json.get("mainFeeTotal").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(json.get("offsiteFeeTotal").decimalValue())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(json.get("totalFeeValue").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
