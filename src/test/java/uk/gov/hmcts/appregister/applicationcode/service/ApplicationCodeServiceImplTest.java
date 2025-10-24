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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;

@ExtendWith(MockitoExtension.class)
public class ApplicationCodeServiceImplTest {

    @Mock private ApplicationCodeRepository repository;

    @Spy private ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapper();

    @Mock private ApplicationFeeService feeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy private final AuditOperationService auditService = new DummyAuditOperationService();

    @Spy private final List<AuditOperationLifecycleListener> auditLifecycleListeners = List.of();

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
        ApplicationCodeDto applicationCodeDto = applicationCodeService.findByCode(code, localDate);

        Assertions.assertEquals(applicationCodeDto.id(), applicationCode.getId());
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
                                applicationCode4));

        String code = "code";
        when(repository.search(code, null, Boolean.FALSE, null, null, criteria))
                .thenReturn(results);

        // execute test
        Page<ApplicationCodeDto> applicationCodeDtoPage =
                applicationCodeService.findAll(code, null, null, Pageable.ofSize(10));

        List<ApplicationCodeDto> pageList = applicationCodeDtoPage.get().toList();

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(pageList.get(0).id(), applicationCode.getId());
        Assertions.assertEquals(pageList.get(1).id(), applicationCode2.getId());
        Assertions.assertEquals(pageList.get(2).id(), applicationCode3.getId());
        Assertions.assertEquals(pageList.get(3).id(), applicationCode4.getId());
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
                                applicationCode4));

        String title = "title";
        when(repository.search(null, title, Boolean.FALSE, null, null, criteria))
                .thenReturn(results);

        // execute test
        Page<ApplicationCodeDto> applicationCodeDtoPage =
                applicationCodeService.findAll(null, title, null, Pageable.ofSize(10));

        List<ApplicationCodeDto> pageList = applicationCodeDtoPage.get().toList();

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(pageList.get(0).id(), applicationCode.getId());
        Assertions.assertEquals(pageList.get(1).id(), applicationCode2.getId());
        Assertions.assertEquals(pageList.get(2).id(), applicationCode3.getId());
        Assertions.assertEquals(pageList.get(3).id(), applicationCode4.getId());
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
                                applicationCode4));

        OffsetDateTime lodgementDate =
                LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDate =
                LocalDate.now().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String title = "title";
        String code = "code";
        when(repository.search(code, title, Boolean.TRUE, lodgementDate, endDate, criteria))
                .thenReturn(results);

        // execute test
        Page<ApplicationCodeDto> applicationCodeDtoPage =
                applicationCodeService.findAll(
                        code, title, LocalDate.now(ZoneOffset.UTC), Pageable.ofSize(10));

        List<ApplicationCodeDto> pageList = applicationCodeDtoPage.get().toList();

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(pageList.get(0).id(), applicationCode.getId());
        Assertions.assertEquals(pageList.get(1).id(), applicationCode2.getId());
        Assertions.assertEquals(pageList.get(2).id(), applicationCode3.getId());
        Assertions.assertEquals(pageList.get(3).id(), applicationCode4.getId());
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
                                applicationCode4));

        OffsetDateTime startDateSearch =
                LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDateSearch =
                LocalDate.now().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        when(repository.search(null, null, Boolean.TRUE, startDateSearch, endDateSearch, criteria))
                .thenReturn(results);

        // execute test
        Page<ApplicationCodeDto> applicationCodeDtoPage =
                applicationCodeService.findAll(
                        null, null, LocalDate.now(ZoneOffset.UTC), Pageable.ofSize(10));

        List<ApplicationCodeDto> pageList = applicationCodeDtoPage.get().toList();

        // make assertion
        Assertions.assertEquals(applicationCodeDtoPage.getTotalElements(), 4);
        Assertions.assertEquals(pageList.get(0).id(), applicationCode.getId());
        Assertions.assertEquals(pageList.get(1).id(), applicationCode2.getId());
        Assertions.assertEquals(pageList.get(2).id(), applicationCode3.getId());
        Assertions.assertEquals(pageList.get(3).id(), applicationCode4.getId());
    }

    class DummyAuditOperationService implements AuditOperationService {

        @Override
        public <T> T processAudit(Optional<Keyable> oldValue, AuditOperation auditType, Function<BaseAuditEvent, Optional<AuditResult<T>>> execution, AuditOperationLifecycleListener... listener) {
            Optional<AuditResult<T>> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT,
                                            UUID.randomUUID().toString(),
                                            Optional.empty(),
                                            Optional.empty()),
                                    "result"));
            return optional.get().getResultingValue();
        }
    }
}
