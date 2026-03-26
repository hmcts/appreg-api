package uk.gov.hmcts.appregister.applicationcode.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapperImpl;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidationSuccess;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidator;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

@ExtendWith(MockitoExtension.class)
public class ApplicationCodeServiceImplTest {

    @Mock private ApplicationCodeRepository repository;
    @Spy private ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapperImpl();

    @Mock private ApplicationFeeService feeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Spy private final List<AuditOperationLifecycleListener> auditLifecycleListeners = List.of();

    @Spy
    private final AuditOperationService auditService =
            new AuditOperationServiceImpl(objectMapper, auditLifecycleListeners);

    @Spy private final PageMapper pageMapper = new PageMapper();

    private final DummyGetApplicationCodeValidator dummyGetApplicationCodeValidator =
            new DummyGetApplicationCodeValidator(repository);

    private ZoneId ukZone;
    private Clock fixedClock;
    private ApplicationCodeServiceImpl applicationCodeService;

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        ukZone = ZoneId.of("Europe/London");
        fixedClock = Clock.fixed(Instant.parse("2024-10-05T10:15:30Z"), ZoneId.of("UTC"));

        applicationCodeService =
                new ApplicationCodeServiceImpl(
                        repository,
                        applicationCodeMapper,
                        feeService,
                        auditService,
                        auditLifecycleListeners,
                        pageMapper,
                        fixedClock,
                        ukZone,
                        dummyGetApplicationCodeValidator);
    }

    @Test
    void findByCode() throws Exception {
        String code = "code";
        LocalDate localDate = LocalDate.now(ZoneOffset.UTC);

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        GetApplicationCodeValidationSuccess success =
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build();
        dummyGetApplicationCodeValidator.setSuccess(success);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        PayloadForGet payloadForGet = PayloadForGet.builder().code(code).date(localDate).build();
        ApplicationCodeGetDetailDto applicationCodeDto =
                applicationCodeService.findByCode(payloadForGet);

        Assertions.assertEquals(applicationCodeDto.getApplicationCode(), applicationCode.getCode());
        verify(feeService).resolveFeePair(applicationCode.getFeeReference(), localDate);
    }

    @Test
    void findByCode_auditsResolvedEntity() {
        String code = "code";
        LocalDate localDate = LocalDate.of(2025, 1, 1);

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        dummyGetApplicationCodeValidator.setSuccess(
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build());
        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        CapturingAuditListener listener = new CapturingAuditListener();
        ApplicationCodeServiceImpl auditedService = buildServiceWithListeners(List.of(listener));

        auditedService.findByCode(PayloadForGet.builder().code(code).date(localDate).build());

        Assertions.assertNotNull(listener.getCompleteEvent());
        Assertions.assertSame(applicationCode, listener.getCompleteEvent().getNewValue());
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
        LocalDate todayUk = LocalDate.now(fixedClock.withZone(ukZone));
        when(repository.search(eq(code), eq(null), eq(todayUk), eq(criteria))).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(code, null, PagingWrapper.of(List.of(), criteria));

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
        LocalDate todayUk = LocalDate.now(fixedClock.withZone(ukZone));
        when(repository.search(eq(null), eq(title), eq(todayUk), eq(criteria))).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(null, title, PagingWrapper.of(List.of(), criteria));

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
        LocalDate todayUk = LocalDate.now(fixedClock.withZone(ukZone));
        when(repository.search(eq(code), eq(title), eq(todayUk), eq(criteria))).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(code, title, PagingWrapper.of(List.of(), criteria));

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
        LocalDate todayUk = LocalDate.now(fixedClock.withZone(ukZone));
        when(repository.search(eq(null), eq(null), eq(todayUk), eq(criteria))).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        null, null, PagingWrapper.of(List.of(), criteria.withPage(0)));

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

    private ApplicationCodeServiceImpl buildServiceWithListeners(
            List<AuditOperationLifecycleListener> listeners) {
        return new ApplicationCodeServiceImpl(
                repository,
                applicationCodeMapper,
                feeService,
                new AuditOperationServiceImpl(objectMapper, listeners),
                listeners,
                pageMapper,
                fixedClock,
                ukZone,
                dummyGetApplicationCodeValidator);
    }

    private static final class CapturingAuditListener implements AuditOperationLifecycleListener {
        private CompleteEvent completeEvent;

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (event instanceof CompleteEvent complete) {
                completeEvent = complete;
            }
        }

        private CompleteEvent getCompleteEvent() {
            return completeEvent;
        }
    }

    class DummyAuditOperationService implements AuditOperationService {
        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(
                    oldValue, auditType, execution, (AuditOperationLifecycleListener) null);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(auditType, execution);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            return processAudit(auditType, execution, listener);
        }

        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            Optional<AuditableResult<T, E>> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT,
                                            UUID.randomUUID().toString(),
                                            null),
                                    "result",
                                    null));
            return optional.get().getResultingValue();
        }
    }

    @Setter
    class DummyGetApplicationCodeValidator extends GetApplicationCodeValidator {
        private GetApplicationCodeValidationSuccess success;

        public DummyGetApplicationCodeValidator(ApplicationCodeRepository repository) {
            super(repository);
        }

        @Override
        public <R> R validate(
                PayloadForGet payload,
                BiFunction<PayloadForGet, GetApplicationCodeValidationSuccess, R> getCode) {
            return getCode.apply(payload, success);
        }
    }
}
