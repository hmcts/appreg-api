package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.testutils.DatabaseReset.SEQUENCE_START_VALUE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ApplicationFeeServiceImplTest extends BaseIntegration {
    private static final long INSERT_TEST_FEE_ID = SEQUENCE_START_VALUE + 67L;
    private static final long UPDATE_TEST_FEE_ID = SEQUENCE_START_VALUE + 68L;

    @Autowired DataAuditRepository dataAuditRepository;

    @Autowired ApplicationFeeService applicationFeeService;

    @Autowired FeeRepository feeRepository;

    private final LocalDate now = LocalDate.now();

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsertFee_insert() {

        val fee = new Fee();
        fee.setId(INSERT_TEST_FEE_ID);
        fee.setReference("CO6.7");
        fee.setDescription("Test Fee");
        fee.setStartDate(now);
        fee.setEndDate(null);
        fee.setOffsite(false);
        fee.setAmount(BigDecimal.valueOf(10.00));

        applicationFeeService.upsertFee(fee);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue("fee", "fee_reference", "CO6.7")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for C06.7 - Fee reference");
                        });
    }

    @Test
    void testUpsertFee_update() {
        createFee();

        val fee = new Fee();
        fee.setId(UPDATE_TEST_FEE_ID);
        fee.setReference("CO6.7");
        fee.setDescription("Test Fee 2");
        fee.setEndDate(now);

        applicationFeeService.upsertFee(fee);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "fee", "fee_reference", "CO6.7", "CO6.7")
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
                        "fee", "fee_description", "Test Fee", "Test Fee 2")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for APP006 - standard_applicant_code");
                        });
    }

    private void createFee() {
        Fee fee = new Fee();
        fee.setReference("CO6.7");
        fee.setDescription("Test Fee");
        fee.setStartDate(now);
        fee.setEndDate(null);
        fee.setOffsite(false);
        fee.setAmount(BigDecimal.valueOf(10.00));
        fee.setChangedDate(LocalDateTime.now().minusDays(2).atOffset(ZoneOffset.UTC));
        fee.setChangedBy(1L);
        fee.setId(UPDATE_TEST_FEE_ID);

        feeRepository.saveAndFlush(fee);
    }
}
