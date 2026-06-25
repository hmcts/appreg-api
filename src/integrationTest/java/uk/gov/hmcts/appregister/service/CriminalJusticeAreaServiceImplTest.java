package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.criminaljusticearea.service.CriminalJusticeService;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class CriminalJusticeAreaServiceImplTest extends BaseIntegration {
    @Autowired private DataAuditRepository dataAuditRepository;

    @Autowired private CriminalJusticeService service;

    @Autowired private CriminalJusticeAreaRepository cjaRepository;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsertCja_insert() {
        val cja = new CriminalJusticeArea();
        cja.setCode("67");
        cja.setDescription("Test cja");

        service.upsertCJA(cja);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "criminal_justice_area", "cja_code", "67")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja code - 67");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "criminal_justice_area", "cja_description", "Test cja")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja description - TESTCJA1");
                        });
    }

    @Test
    void testUpsertCja_update() {
        createCja("67", "Test CJA");

        val cja = new CriminalJusticeArea();
        cja.setCode("67");
        cja.setDescription("Test cja 2");

        service.upsertCJA(cja);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "criminal_justice_area", "cja_code", "67", cja.getCode())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja code - 67");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "criminal_justice_area",
                        "cja_description",
                        "Test CJA",
                        cja.getDescription())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja - description");
                        });
    }

    @Test
    void testUpsertCja_updateCaseInsensitive() {
        createCja("AA", "Test CJA");

        val cja = new CriminalJusticeArea();
        cja.setCode("aa");
        cja.setDescription("Test cja 2");

        service.upsertCJA(cja);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "criminal_justice_area", "cja_code", "AA", cja.getCode())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja code - AA");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "criminal_justice_area",
                        "cja_description",
                        "Test CJA",
                        cja.getDescription())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for cja - description");
                        });
    }

    private void createCja(String code, String description) {
        val criminalJusticeArea = new CriminalJusticeArea();
        criminalJusticeArea.setDescription(description);
        criminalJusticeArea.setCode(code);

        cjaRepository.saveAndFlush(criminalJusticeArea);
    }
}
