package uk.gov.hmcts.appregister.service;

import lombok.val;
import static org.mockito.Mockito.when;
import uk.gov.hmcts.appregister.applicationcode.service.ApplicationCodeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ApplicationCodeServiceImplTest extends BaseIntegration {
    @Autowired
    private ApplicationCodeService service;

    @Autowired
    private ApplicationCodeRepository repository;

    @Autowired
    private DataAuditRepository dataAuditRepository;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
            .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpsertResolutionCode_insert() {

        val applicationCode = new ApplicationCode();
        applicationCode.setCode("TESTAC1");
        applicationCode.setTitle("Test Application code 2");
        applicationCode.setStartDate(LocalDate.now());
        applicationCode.setEndDate(null);
        applicationCode.setVersion(1L);
        applicationCode.setWording("");
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);

        service.upsertApplicationCode(applicationCode);

        dataAuditRepository
            .findDataAuditForTableAndColumnAndNewValue(
                "application_codes", "application_code", "TESTAC1")
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
        createApplicationCode();

        val applicationCode = new ApplicationCode();
        applicationCode.setCode("TESTAC1");
        applicationCode.setTitle("Test applicationCode 2");
        applicationCode.setEndDate(LocalDate.now());

        service.upsertApplicationCode(applicationCode);

        dataAuditRepository
            .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                "application_codes",
                "application_code_title",
                "Test Application Code",
                applicationCode.getTitle())
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

    private void createApplicationCode() {
        val applicationCode = new ApplicationCode();
        applicationCode.setCode("TESTAC1");
        applicationCode.setTitle("Test Application Code");
        applicationCode.setStartDate(LocalDate.now());
        applicationCode.setEndDate(null);
        applicationCode.setVersion(1L);
        applicationCode.setWording("");
        applicationCode.setChangedBy(1L);
        applicationCode.setChangedDate(OffsetDateTime.now());
        applicationCode.setFeeDue(YesOrNo.NO);
        applicationCode.setRequiresRespondent(YesOrNo.NO);
        applicationCode.setBulkRespondentAllowed(YesOrNo.NO);

        repository.saveAndFlush(applicationCode);
    }
}
