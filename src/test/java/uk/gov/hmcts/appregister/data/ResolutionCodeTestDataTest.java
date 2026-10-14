package uk.gov.hmcts.appregister.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

class ResolutionCodeTestDataTest {
    @Test
    void
            given_multipleFixtureInstances_when_generate_then_useDistinctBusinessCodesWithinColumnLength() {
        var records =
                IntStream.range(0, 128)
                        .mapToObj(index -> new ResolutionCodeTestData().someComplete())
                        .toList();

        assertThat(records)
                .extracting(ResolutionCode::getResultCode)
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).matches("TR[0-9a-z]{1,7}"));
    }
}
