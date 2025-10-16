package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class ApplicationListRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListRepository repository;
    @Autowired private CriminalJusticeAreaRepository cjaRepository;

    private static final LocalDate DEFAULT_DATE = LocalDate.of(2025, 1, 2);
    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    private void save(
            String status,
            String courtCode,
            CriminalJusticeArea cja,
            LocalDate date,
            LocalTime time,
            String description,
            String otherLocation) {

        ApplicationList al =
                ApplicationList.builder()
                        .status(ApplicationListStatus.fromValue(status))
                        .description(description)
                        .otherLocation(otherLocation)
                        .courtName(courtCode != null ? "Court " + courtCode : null)
                        .courtCode(courtCode)
                        .cja(cja)
                        .date(date)
                        .time(time)
                        .durationHours((short) 0)
                        .durationMinutes((short) 0)
                        .build();
        repository.saveAndFlush(al);
    }

    private CriminalJusticeArea saveCja(String code, String desc) {
        CriminalJusticeArea cja =
                CriminalJusticeArea.builder().code(code).description(desc).build();
        return cjaRepository.saveAndFlush(cja);
    }

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    private ApplicationList buildEntity() {
        return ApplicationList.builder()
                .status(ApplicationListStatus.OPEN)
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
        expectAllCommonEntityFields(saved, reloaded);
        assertThat(reloaded.getUuid()).isEqualTo(saved.getUuid());
        assertThat(reloaded.getDescription()).isEqualTo("Smoke test list");
        assertThat(reloaded.getCourtName()).isEqualTo("Cardiff Crown Court");
        assertThat(reloaded.getCourtCode()).isEqualTo("CCC003");
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(reloaded.getCreatedUser()).isEqualTo(TokenGenerator.DEFAULT_USERNAME);
        assertThat(reloaded.getChangedBy())
                .isEqualTo(TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID);
        assertThat(reloaded.isDeleted()).isFalse();
        assertThat(reloaded.getDeletedBy()).isNull();
        assertThat(reloaded.getDeletedDate()).isNull();
    }

    @Test
    @Transactional
    void deleteApplicationList() {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    // save a new entity
                    var saved = repository.saveAndFlush(buildEntity());
                    var reloaded = repository.findById(saved.getPk()).orElseThrow();

                    reloaded.setDeleted(true);

                    // delete the entity
                    repository.saveAndFlush(reloaded);

                    // prove the deletion attributes are set
                    assertThat(reloaded.getDeletedBy())
                            .isEqualTo(
                                    TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID);

                    expectAllCommonEntityFields(saved, reloaded);
                    assertThat(reloaded.getDeletedDate()).isNotNull();
                    assertThat(reloaded.isDeleted()).isTrue();

                    // prove the core entity values have not changed
                    assertThat(reloaded.getUuid()).isEqualTo(saved.getUuid());
                    assertThat(reloaded.getDescription()).isEqualTo("Smoke test list");
                    assertThat(reloaded.getCourtName()).isEqualTo("Cardiff Crown Court");
                    assertThat(reloaded.getCourtCode()).isEqualTo("CCC003");
                    assertThat(reloaded.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
                    assertThat(reloaded.getCreatedUser())
                            .isEqualTo(TokenGenerator.DEFAULT_USERNAME);
                    assertThat(reloaded.getChangedBy())
                            .isEqualTo(
                                    TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID);
                });
        System.out.println();
    }

    @Test
    @DisplayName("findAllByFilter: matches by calendar day + hh:mm only")
    void findAllByFilter_dateAndTime_match() {
        // Given
        LocalDate targetDay = LocalDate.of(2025, 1, 2);
        LocalTime nineAm = LocalTime.of(9, 0);
        LocalTime tenAm = LocalTime.of(10, 0);

        // Matching row: correct day AND 09:00
        save("OPEN", "CCC003", null, targetDay, nineAm, "keep", "west");

        // Wrong time (10:00) on same day -> should NOT match
        save("OPEN", "CCC003", null, targetDay, tenAm, "drop", "west");

        // Right time (09:00) but wrong day -> should NOT match
        LocalDate otherDay = targetDay.plusDays(1);
        save("OPEN", "CCC003", null, otherDay, nineAm, "drop", "west");

        Pageable page = PageRequest.of(0, 10);

        // When: filter ONLY by date and time; leave other filters null
        Page<ApplicationList> result =
                repository.findAllByFilter(
                        null, // status
                        null, // courtCode
                        null, // cja
                        targetDay, // date
                        nineAm, // time
                        null, // description
                        null, // other location
                        page);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        ApplicationList only = result.getContent().getFirst();
        assertThat(only.getDate()).isEqualTo(targetDay);
        assertThat(only.getTime()).isEqualTo(nineAm);
    }

    @Test
    @DisplayName("findAllByFilter: status + courtCode filters match")
    void findAllByFilter_statusAndCourtCode_match() {
        // Given

        save("OPEN", "CCC003", null, DEFAULT_DATE, DEFAULT_TIME, "keep", "west");
        save("CLOSED", "CCC003", null, DEFAULT_DATE, DEFAULT_TIME, "drop", "west");
        save("OPEN", "OTHER01", null, DEFAULT_DATE, DEFAULT_TIME, "drop", "west");

        Pageable page = PageRequest.of(0, 10);

        // When
        Page<ApplicationList> result =
                repository.findAllByFilter(
                        ApplicationListStatus.OPEN, "CCC003", null, null, null, null, null, page);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getCourtCode()).isEqualTo("CCC003");
        assertThat(result.getContent().getFirst().getStatus())
                .isEqualTo(ApplicationListStatus.OPEN);
    }

    @Test
    @DisplayName("findAllByFilter: CJA entity equality filter matches exactly")
    void findAllByFilter_cja_match() {
        // Given
        var cja52 = saveCja("52", "CJA 52");
        var cja53 = saveCja("53", "CJA 53");

        save("OPEN", null, cja52, DEFAULT_DATE, DEFAULT_TIME, "keep", "loc");
        save("OPEN", null, cja53, DEFAULT_DATE, DEFAULT_TIME, "drop", "loc");

        // When
        Page<ApplicationList> result =
                repository.findAllByFilter(
                        ApplicationListStatus.OPEN,
                        null,
                        cja52,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10));

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getCja().getCode()).isEqualTo("52");
    }

    @Test
    @DisplayName("findAllByFilter: description contains is case-insensitive")
    void findAllByFilter_description_contains_caseInsensitive() {
        // Given
        var date = LocalDate.of(2025, 1, 3);
        var time = LocalTime.of(11, 0);

        save("OPEN", null, null, date, time, "Morning Session", "Hall");
        save("OPEN", null, null, date, time, "Afternoon list", "Hall");

        // When
        Page<ApplicationList> result =
                repository.findAllByFilter(
                        ApplicationListStatus.OPEN,
                        null,
                        null,
                        null,
                        null,
                        "SESSION",
                        null,
                        PageRequest.of(0, 10));

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("Morning Session");
    }

    @Test
    @DisplayName("findAllByFilter: otherLocation contains is case-insensitive")
    void findAllByFilter_otherLocation_contains_caseInsensitive() {
        // Given

        save("OPEN", null, null, DEFAULT_DATE, DEFAULT_TIME, "x", "Town Hall");
        save("OPEN", null, null, DEFAULT_DATE, DEFAULT_TIME, "x", "Library Room");

        // When
        Page<ApplicationList> result =
                repository.findAllByFilter(
                        null, null, null, null, null, null, "hall", PageRequest.of(0, 10));

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getOtherLocation()).isEqualTo("Town Hall");
    }

    @Test
    @DisplayName("findAllByFilter: paging works (page size 1, sorted by date asc)")
    void findAllByFilter_paging_andSorting() {
        // Given
        var d1 = LocalDate.of(2025, 6, 1);
        var d2 = LocalDate.of(2025, 6, 2);
        var t = LocalTime.of(11, 0);

        save("OPEN", "PG1", null, d1, t, "first", "loc");
        save("OPEN", "PG1", null, d2, t, "second", "loc");

        // When: page 0 size 1
        Page<ApplicationList> page0 =
                repository.findAllByFilter(
                        ApplicationListStatus.OPEN,
                        "PG1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(
                                0, 1, org.springframework.data.domain.Sort.by("date").ascending()));

        // And: page 1 size 1
        Page<ApplicationList> page1 =
                repository.findAllByFilter(
                        ApplicationListStatus.OPEN,
                        "PG1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(
                                1, 1, org.springframework.data.domain.Sort.by("date").ascending()));

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getNumberOfElements()).isEqualTo(1);
        assertThat(page1.getNumberOfElements()).isEqualTo(1);

        // Confirm ordering by date asc
        assertThat(page0.getContent().getFirst().getDate()).isEqualTo(d1);
        assertThat(page1.getContent().getFirst().getDate()).isEqualTo(d2);
    }
}
