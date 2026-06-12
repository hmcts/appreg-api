package uk.gov.hmcts.appregister.common.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;

class TemplateableCoverageTest {

    @Test
    void doesSubstitute_returnsTrueWhenValidationSucceeds() {
        var templateable = new TestTemplateable();

        assertTrue(templateable.doesSubstitute("ok"));
        assertEquals("ok", templateable.lastSeenValue().get());
    }

    @Test
    void doesSubstitute_returnsFalseWhenValidationThrows() {
        var templateable = new TestTemplateable();

        assertFalse(templateable.doesSubstitute("fail"));
    }

    private static final class TestTemplateable implements Templateable {
        private final AtomicReference<String> lastSeenValue = new AtomicReference<>();

        @Override
        public String getValue() {
            return "value";
        }

        @Override
        public TemplateKeyWithConstraint getDetail() {
            return new TemplateKeyWithConstraint();
        }

        @Override
        public void canValueBeSubstituted(String value) {
            lastSeenValue.set(value);
            if ("fail".equals(value)) {
                throw new IllegalArgumentException("bad value");
            }
        }

        @Override
        public boolean isSubstitutionComplete() {
            return false;
        }

        AtomicReference<String> lastSeenValue() {
            return lastSeenValue;
        }
    }
}
