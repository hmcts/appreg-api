package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.testutils.DatabaseReset.SEQUENCE_START_VALUE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.standardapplicant.service.StandardApplicantService;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class StandardApplicationServiceImplTest extends BaseIntegration {
    private static final long INSERT_TEST_STANDARD_APPLICANT_ID = SEQUENCE_START_VALUE + 67L;
    private static final long UPDATE_TEST_STANDARD_APPLICANT_ID = 7L;

    @Autowired private StandardApplicantService standardApplicantService;

    @Autowired private StandardApplicantRepository standardApplicantRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    private final LocalDate now = LocalDate.now();

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsertStandardApplicant_insert() {

        val standardApplicant = new StandardApplicant();
        standardApplicant.setId(INSERT_TEST_STANDARD_APPLICANT_ID);
        standardApplicant.setApplicantCode("SW99001");
        standardApplicant.setName("Craig Smith");
        standardApplicant.setApplicantStartDate(now);
        standardApplicant.setApplicantEndDate(now.plusDays(1));
        standardApplicant.setAddressLine1("123 Main St");
        standardApplicant.setPostcode("AB12 3CD");

        standardApplicantService.upsertStandardApplicant(standardApplicant);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "standard_applicants", "standard_applicant_code", "SW99001")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getOldValue().isEmpty();
                            assert dataAudit.getNewValue().equals("SW99001");
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError("Data audit record not found for SW99001");
                        });
    }

    @Test
    void testUpsertStandardApplicant_update() {

        val standardApplicant = new StandardApplicant();
        standardApplicant.setId(UPDATE_TEST_STANDARD_APPLICANT_ID);
        standardApplicant.setApplicantCode("APP006");
        standardApplicant.setName("John Deer");
        standardApplicant.setApplicantStartDate(now);
        standardApplicant.setApplicantEndDate(now.plusDays(1));

        standardApplicantService.upsertStandardApplicant(standardApplicant);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "standard_applicants", "standard_applicant_code", "APP006", "APP006")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for APP006 - standard_applicant_code");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "standard_applicants", "name", "Organisation 3", "John Deer")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for APP006 - name");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "standard_applicants",
                        "standard_applicant_start_date",
                        LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for APP006 - standard_applicant_start_date");
                        });

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "standard_applicants",
                        "standard_applicant_end_date",
                        "",
                        LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for APP006 - standard_applicant_end_date");
                        });
    }
}
