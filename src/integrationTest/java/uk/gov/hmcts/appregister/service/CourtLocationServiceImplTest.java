package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.testutils.DatabaseReset.SEQUENCE_START_VALUE;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class CourtLocationServiceImplTest extends BaseIntegration {
    private static final long INSERT_TEST_COURTHOUSE_ID = SEQUENCE_START_VALUE + 67L;
    private static final long UPDATE_TEST_COURTHOUSE_ID = SEQUENCE_START_VALUE + 68L;

    @Autowired private CourtLocationService service;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Autowired private NationalCourtHouseRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsert_insert() {
        val nationalCourtHouse = new NationalCourtHouse();
        nationalCourtHouse.setId(INSERT_TEST_COURTHOUSE_ID);
        nationalCourtHouse.setCourtLocationCode("TEST001");
        nationalCourtHouse.setCourtType("CHOA");
        nationalCourtHouse.setName("Unit Test Court");
        nationalCourtHouse.setStartDate(LocalDate.now());
        nationalCourtHouse.setEndDate(null);

        service.upsertCourtHouse(nationalCourtHouse);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "national_court_houses",
                        "court_location_code",
                        nationalCourtHouse.getCourtLocationCode())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for courtHouse code - TEST001");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "national_court_houses", "courthouse_name", nationalCourtHouse.getName())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for courtHouse - name");
                        });
    }

    @Test
    void testUpsert_update() {
        createCourtHouse();

        val courtHouse = new NationalCourtHouse();
        courtHouse.setCourtLocationCode("TEST001");
        courtHouse.setCourtType("CHOA");
        courtHouse.setName("Test court 2");
        courtHouse.setId(UPDATE_TEST_COURTHOUSE_ID);

        service.upsertCourtHouse(courtHouse);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "national_court_houses",
                        "court_location_code",
                        "TEST001",
                        courtHouse.getCourtLocationCode())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for courtHouse code - TEST001");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "national_court_houses",
                        "courthouse_name",
                        "Test Court",
                        courtHouse.getName())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for courtHouse - name");
                        });
    }

    private void createCourtHouse() {
        val nationalCourtHouse = new NationalCourtHouse();
        nationalCourtHouse.setCourtLocationCode("TEST001");
        nationalCourtHouse.setCourtType("CHOA");
        nationalCourtHouse.setName("Test Court");
        nationalCourtHouse.setStartDate(LocalDate.now());
        nationalCourtHouse.setEndDate(null);
        nationalCourtHouse.setChangedDate(OffsetDateTime.now());
        nationalCourtHouse.setChangedBy(1L);
        nationalCourtHouse.setId(UPDATE_TEST_COURTHOUSE_ID);

        repository.saveAndFlush(nationalCourtHouse);
    }
}
