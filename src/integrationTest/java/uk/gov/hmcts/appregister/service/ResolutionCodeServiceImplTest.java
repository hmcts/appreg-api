package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.resultcode.service.ResultCodeService;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ResolutionCodeServiceImplTest extends BaseIntegration {
    @Autowired private DataAuditRepository dataAuditRepository;

    @Autowired private ResultCodeService resultCodeService;

    @Autowired private ResolutionCodeRepository resolutionCodeRepository;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsertResolutionCode_insert() {

        val resolutionCode = new ResolutionCode();
        resolutionCode.setResultCode("TESTRC1");
        resolutionCode.setTitle("Test resolutionCode");
        resolutionCode.setStartDate(LocalDate.now());
        resolutionCode.setEndDate(null);
        resolutionCode.setVersion(1L);
        resolutionCode.setWording("");

        resultCodeService.upsertResultCode(resolutionCode);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndNewValue(
                        "resolution_codes", "resolution_code", "TESTRC1")
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.CREATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for TESTRC1 - resolution_code");
                        });
    }

    @Test
    void testUpsertResolutionCode_update() {
        createResolutionCode();

        val resolutionCode = new ResolutionCode();
        resolutionCode.setResultCode("TESTRC1");
        resolutionCode.setTitle("Test resolutionCode 2");
        resolutionCode.setEndDate(LocalDate.now());

        resultCodeService.upsertResultCode(resolutionCode);

        dataAuditRepository
                .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                        "resolution_codes",
                        "resolution_code_title",
                        "Test Resolution Code",
                        resolutionCode.getTitle())
                .ifPresentOrElse(
                        dataAudit -> {
                            // Assert that the audit record has the expected values
                            assert dataAudit.getUpdateType().equals(CrudEnum.UPDATE);
                        },
                        () -> {
                            throw new AssertionError(
                                    "Data audit record not found for Resolution Code - title");
                        });
    }

    private void createResolutionCode() {
        val resolutionCode = new ResolutionCode();
        resolutionCode.setResultCode("TESTRC1");
        resolutionCode.setTitle("Test Resolution Code");
        resolutionCode.setStartDate(LocalDate.now());
        resolutionCode.setEndDate(null);
        resolutionCode.setVersion(1L);
        resolutionCode.setWording("");
        resolutionCode.setChangedBy(1L);
        resolutionCode.setChangedDate(OffsetDateTime.now());

        resolutionCodeRepository.saveAndFlush(resolutionCode);
    }
}
