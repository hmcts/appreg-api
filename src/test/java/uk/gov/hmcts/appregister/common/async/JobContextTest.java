package uk.gov.hmcts.appregister.common.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobContextTest {

    @Test
    void defaults_haveNoFailuresAndContinueValidating() {
        JobContext jobContext = new JobContext();

        assertThat(jobContext.getValidationFailureMessages()).isEmpty();
        assertThat(jobContext.hasFailure()).isFalse();
        assertThat(jobContext.getCommaDelimitedFailureMessage()).isEmpty();
        assertThat(jobContext.isStoppedValidating()).isFalse();
    }

    @Test
    void logFailure_addsUniqueMessagesAndMarksFailure() {
        JobContext jobContext = new JobContext();

        jobContext.logFailure("first");
        jobContext.logFailure("first");
        jobContext.logFailure("second");

        assertThat(jobContext.getValidationFailureMessages()).containsExactly("first", "second");
        assertThat(jobContext.hasFailure()).isTrue();
        assertThat(jobContext.getCommaDelimitedFailureMessage()).isEqualTo("first, second");
    }

    @Test
    void setters_replaceMessagesAndStopValidation() {
        JobContext jobContext = new JobContext();

        jobContext.setValidationFailureMessages(List.of("replacement"));
        jobContext.setStoppedValidating(true);

        assertThat(jobContext.getValidationFailureMessages()).containsExactly("replacement");
        assertThat(jobContext.hasFailure()).isTrue();
        assertThat(jobContext.getCommaDelimitedFailureMessage()).isEqualTo("replacement");
        assertThat(jobContext.isStoppedValidating()).isTrue();
    }
}
