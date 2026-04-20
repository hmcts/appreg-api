package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import lombok.val;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

class ApplicationListControllerAuditFailureTest extends AbstractApplicationListControllerCrudTest {

    @MockitoBean private DataAuditRepository dataAuditRepository;

    @Test
    void givenAuditPersistenceFails_whenCreateApplicationList_then201AndListStillPersists()
            throws Exception {
        val token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        val req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("List survives audit failure")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        val auditFailureLog = LogCaptor.forClass(AuditOperationServiceImpl.class);
        auditFailureLog.clearLogs();

        when(dataAuditRepository.save(any(DataAudit.class)))
                .thenThrow(new RuntimeException("audit persistence failed"));

        val createResponse =
                restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        createResponse.then().statusCode(HttpStatus.CREATED.value());
        val created = createResponse.as(ApplicationListGetDetailDto.class);

        // Fetch the created list through the real endpoint to prove the business write committed.
        val fetched = getApplicationListDetail(created.getId(), token);

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getDescription()).isEqualTo(req.getDescription());
        assertThat(fetched.getCourtCode()).isEqualTo(VALID_COURT_CODE);

        verify(dataAuditRepository, atLeastOnce()).save(any(DataAudit.class));
        assertThat(auditFailureLog.getErrorLogs())
                .anyMatch(log -> log.contains("Audit listener failure suppressed."));
    }
}
