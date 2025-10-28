package uk.gov.hmcts.appregister.applicationfee.model;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.FeePair;

/**
 * Unit tests for {@link FeePair}.
 *
 * <p>This class provides **minimal test coverage** to satisfy the SonarQube quality gate, since
 * {@code FeePair} is a Java record with no additional logic.
 *
 * <p>Why this exists: - Sonar considers FeePair "new code" → requires test coverage >= 80%.
 *
 * <p>Notes for future maintainers: - These tests are intentionally lightweight; they are not
 * business-critical. - When FeePair gains behaviour or validation logic, expand these tests
 * accordingly. - If FeePair is temporary (proof-of-concept), these tests can be removed along with
 * the record.
 */
public class FeePairTest {
    @Test
    void constructorAndAccessors_workWithNulls() {
        FeePair pair = new FeePair(null, null);

        assertNull(pair.mainFee());
        assertNull(pair.offsiteFee());
    }
}
