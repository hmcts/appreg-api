package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class ApplicationListRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListRepository repository;

    private ApplicationList buildEntity() {
        return ApplicationList.builder()
                .status("OPEN")
                .description("Smoke test list")
                .courtName("Cardiff Crown Court")
                .courtCode("CCC003")
                .date(LocalDate.of(2025, 1, 1))
                .time(LocalTime.of(9, 0))
                .durationHours((short) 1)
                .durationMinutes((short) 30)
                .build();
    }

    @Test
    @DisplayName("save + reload yields generated PK, UUID, and version=0")
    void saveAndReload() {
        var saved = repository.saveAndFlush(buildEntity());

        assertThat(saved.getPk()).isNotNull();
        assertThat(saved.getUuid()).isNotNull();
        assertThat(saved.getVersion()).isZero();

        var reloaded = repository.findById(saved.getPk()).orElseThrow();
        assertThat(reloaded.getUuid()).isEqualTo(saved.getUuid());
        assertThat(reloaded.getDescription()).isEqualTo("Smoke test list");
        assertThat(reloaded.getCourtName()).isEqualTo("Cardiff Crown Court");
        assertThat(reloaded.getCourtCode()).isEqualTo("CCC003");
        assertThat(reloaded.getStatus()).isEqualTo("OPEN");
        assertThat(reloaded.getCreatedUser()).isEqualTo(TokenGenerator.DEFAULT_USERNAME);
        assertThat(reloaded.getChangedBy())
                .isEqualTo(TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID);
    }
}