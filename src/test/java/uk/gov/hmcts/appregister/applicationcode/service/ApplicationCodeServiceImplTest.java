package uk.gov.hmcts.appregister.applicationcode.service;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapperImpl;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

@ExtendWith(MockitoExtension.class)
public class ApplicationCodeServiceImplTest {

    @Mock private ApplicationCodeRepository repository;

    @Spy private ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapperImpl();

    @Mock private ApplicationFeeService feeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy private final AuditOperationService auditService = new DummyAuditOperationService();

    @Spy private final List<AuditOperationLifecycleListener> auditLifecycleListeners = List.of();

    @Spy private final PageMapper pageMapper = new PageMapper();

    @InjectMocks private ApplicationCodeServiceImpl applicationCodeService;

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void findByCode() throws Exception {
        String code = "code";
        LocalDate localDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate offsetDateTime = LocalDate.now();

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        when(repository.findByCodeAndDate(code, offsetDateTime))
                .thenReturn(List.of(applicationCode));
        ApplicationCodeGetDetailDto applicationCodeDto =
                applicationCodeService.findByCode(code, localDate);

        Assertions.assertEquals(applicationCodeDto.getApplicationCode(), applicationCode.getCode());
    }

    @Test
    void findAllByCode() throws Exception {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode2 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode3 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode4 = new ApplicationCodeTestData().someComplete();

        Pageable criteria = Pageable.ofSize(10);
        PageImpl<ApplicationCode> results =
                new PageImpl<>(
                        List.of(
                                applicationCode,
                                applicationCode2,
                                applicationCode3,
                                applicationCode4),
                        Pageable.ofSize(4).withPage(0),
                        4);

        String code = "code";
        when(repository.search(code, null, criteria)).thenReturn(results);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(code, null, Pageable.ofSize(10));

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(0).getApplicationCode(),
                applicationCode.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(1).getApplicationCode(),
                applicationCode2.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    @Test
    void findAllByTitle() throws Exception {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode2 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode3 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode4 = new ApplicationCodeTestData().someComplete();

        Pageable criteria = Pageable.ofSize(10);
        PageImpl<ApplicationCode> results =
                new PageImpl<>(
                        List.of(
                                applicationCode,
                                applicationCode2,
                                applicationCode3,
                                applicationCode4),
                        Pageable.ofSize(4).withPage(0),
                        4);

        String title = "title";
        when(repository.search(null, title, criteria)).thenReturn(results);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(null, title, Pageable.ofSize(10));

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(0).getApplicationCode(),
                applicationCode.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(1).getApplicationCode(),
                applicationCode2.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    @Test
    void findAllByDate() throws Exception {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode2 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode3 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode4 = new ApplicationCodeTestData().someComplete();

        Pageable criteria = Pageable.ofSize(10);
        PageImpl<ApplicationCode> results =
                new PageImpl<>(
                        List.of(
                                applicationCode,
                                applicationCode2,
                                applicationCode3,
                                applicationCode4),
                        Pageable.ofSize(4).withPage(0),
                        4);

        String title = "title";
        String code = "code";
        when(repository.search(code, title, criteria)).thenReturn(results);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(code, title, Pageable.ofSize(10));

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(0).getApplicationCode(),
                applicationCode.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(1).getApplicationCode(),
                applicationCode2.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    @Test
    void findAllCriteria() throws Exception {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode2 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode3 = new ApplicationCodeTestData().someComplete();
        ApplicationCode applicationCode4 = new ApplicationCodeTestData().someComplete();

        Pageable criteria = Pageable.ofSize(10);
        PageImpl<ApplicationCode> results =
                new PageImpl<>(
                        List.of(
                                applicationCode,
                                applicationCode2,
                                applicationCode3,
                                applicationCode4),
                        Pageable.ofSize(4).withPage(0),
                        4);
        when(repository.search(null, null, criteria)).thenReturn(results);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(null, null, Pageable.ofSize(10).withPage(0));

        // make assertion
        Assertions.assertEquals(4, applicationCodeDtoPage.getTotalElements());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(0).getApplicationCode(),
                applicationCode.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(1).getApplicationCode(),
                applicationCode2.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        Assertions.assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    class DummyAuditOperationService implements AuditOperationService {
        @Override
        public <T> T processAudit(
                AuditEventEnum auditType,
                Function<BaseAuditEvent, Optional<T>> execution,
                AuditOperationLifecycleListener... listener) {
            Optional<T> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT,
                                            UUID.randomUUID().toString()),
                                    "result"));
            return optional.orElse(null);
        }
    }
}
