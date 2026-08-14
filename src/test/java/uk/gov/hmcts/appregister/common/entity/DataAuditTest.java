package uk.gov.hmcts.appregister.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.validation.Validation;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class DataAuditTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000001:11111111-1111-1111-1111-111111111111";

    @Test
    void givenLongDisplayUsernameAndDirectoryId_whenValidating_thenNoConstraintViolation()
            throws NoSuchFieldException {
        var audit =
                DataAudit.builder()
                        .schemaName("appreg")
                        .tableName("application_list_entries")
                        .columnName("notes")
                        .createdUser("a".repeat(200) + "@email.test")
                        .changedBy(USER_ID)
                        .changedDate(OffsetDateTime.now())
                        .updateType(CrudEnum.UPDATE)
                        .build();

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(audit)).isEmpty();
        }

        assertThat(
                        DataAudit.class
                                .getDeclaredField("createdUser")
                                .getAnnotation(Column.class)
                                .name())
                .isEqualTo("user_name");
        assertThat(DataAudit.class.getDeclaredField("changedBy").getAnnotation(Column.class).name())
                .isEqualTo("user_id");
    }
}
