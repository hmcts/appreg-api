package uk.gov.hmcts.appregister.applicationcode.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapperImpl;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidationSuccess;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidator;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import utils.CurrencyUtil;

@ExtendWith(MockitoExtension.class)
class ApplicationCodeServiceImplTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2024-10-05T10:15:30Z");
    private static final LocalDate FIXED_BUSINESS_DATE = LocalDate.of(2024, Month.OCTOBER, 5);
    private static final LocalDate REQUEST_DATE = LocalDate.of(2024, Month.OCTOBER, 5);

    @Mock private ApplicationCodeRepository repository;
    @Spy private ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapperImpl();

    @Mock private ApplicationFeeService feeService;

    @Spy private final List<AuditOperationLifecycleListener> auditLifecycleListeners = List.of();

    @Spy
    private final AuditOperationService auditService =
            new AuditOperationServiceImpl(auditLifecycleListeners);

    @Spy private final PageMapper pageMapper = new PageMapper();

    private final DummyGetApplicationCodeValidator dummyGetApplicationCodeValidator =
            new DummyGetApplicationCodeValidator(repository);

    private ZoneId ukZone;
    private Clock fixedClock;
    private ApplicationCodeServiceImpl applicationCodeService;

    @BeforeEach
    void setup() {
        ukZone = ZoneId.of("Europe/London");
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

        applicationCodeService =
                new ApplicationCodeServiceImpl(
                        repository,
                        applicationCodeMapper,
                        feeService,
                        auditService,
                        pageMapper,
                        fixedClock,
                        ukZone,
                        dummyGetApplicationCodeValidator);
    }

    @Test
    void findByCode() {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        GetApplicationCodeValidationSuccess success =
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build();
        dummyGetApplicationCodeValidator.setSuccess(success);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();

        when(feeService.resolveFeePair(Mockito.notNull(), Mockito.notNull()))
                .thenReturn(new FeePair(dummyMain, dummyOffset));

        String code = "code";

        LocalDate localDate = REQUEST_DATE;

        PayloadForGet payloadForGet = PayloadForGet.builder().code(code).date(localDate).build();
        ApplicationCodeGetDetailDto applicationCodeDto =
                applicationCodeService.findByCode(payloadForGet);

        assertEquals(applicationCodeDto.getApplicationCode(), applicationCode.getCode());

        assertEquals(
                CurrencyUtil.getPoundsToPennies(dummyMain.getAmount()),
                applicationCodeDto.getFeeAmount().get().getValue());
        assertEquals(
                CurrencyUtil.getPoundsToPennies(dummyOffset.getAmount()),
                applicationCodeDto.getOffsiteFeeAmount().get().getValue());
    }

    @Test
    void findByCode_auditsRequestedLookupCriteria() {

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();

        when(feeService.resolveFeePair(Mockito.notNull(), Mockito.notNull()))
                .thenReturn(new FeePair(dummyMain, dummyOffset));

        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setStartDate(LocalDate.of(2020, Month.JANUARY, 1));
        dummyGetApplicationCodeValidator.setSuccess(
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build());
        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        CapturingAuditListener listener = new CapturingAuditListener();
        ApplicationCodeServiceImpl auditedService = buildServiceWithListeners(List.of(listener));

        String code = "code";
        LocalDate localDate = LocalDate.of(2025, Month.JANUARY, 1);

        auditedService.findByCode(PayloadForGet.builder().code(code).date(localDate).build());

        assertNotNull(listener.getCompleteEvent());
        ApplicationCode audited = (ApplicationCode) listener.getCompleteEvent().getNewValue();
        assertNotSame(applicationCode, audited);
        assertEquals(code, audited.getCode());
        assertEquals(localDate, audited.getStartDate());
    }

    @Test
    void findByCodeNullDate() {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        GetApplicationCodeValidationSuccess success =
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build();
        dummyGetApplicationCodeValidator.setSuccess(success);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();

        when(feeService.resolveFeePair(Mockito.notNull(), Mockito.isNull()))
                .thenReturn(new FeePair(dummyMain, dummyOffset));

        String code = "code";
        LocalDate localDate = null;

        PayloadForGet payloadForGet = PayloadForGet.builder().code(code).date(localDate).build();
        ApplicationCodeGetDetailDto applicationCodeDto =
                applicationCodeService.findByCode(payloadForGet);

        assertEquals(applicationCodeDto.getApplicationCode(), applicationCode.getCode());
        assertEquals(
                CurrencyUtil.getPoundsToPennies(dummyMain.getAmount()),
                applicationCodeDto.getFeeAmount().get().getValue());
        assertEquals(
                CurrencyUtil.getPoundsToPennies(dummyOffset.getAmount()),
                applicationCodeDto.getOffsiteFeeAmount().get().getValue());
    }

    @Test
    void findAllByCode() {
        var applicationCode = createApplicationCodeWithFeeReference("REF-1");
        var applicationCode2 = createApplicationCodeWithFeeReference("REF-2");
        var applicationCode3 = createApplicationCodeWithFeeReference("REF-3");
        var applicationCode4 = createApplicationCodeWithFeeReference("REF-4");

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
        LocalDate todayUk = FIXED_BUSINESS_DATE;
        when(repository.search(code, null, todayUk, criteria)).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();
        var feePairs =
                Map.of(
                        "REF-1", new FeePair(dummyMain, dummyOffset),
                        "REF-2", new FeePair(dummyMain, dummyOffset),
                        "REF-3", new FeePair(dummyMain, dummyOffset),
                        "REF-4", new FeePair(dummyMain, dummyOffset));

        when(feeService.resolveFeePairs(List.of("REF-1", "REF-2", "REF-3", "REF-4"), todayUk))
                .thenReturn(feePairs);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        code, null, null, PagingWrapper.of(List.of(), criteria));

        // make assertion
        assertEquals(4, applicationCodeDtoPage.getTotalElements());
        assertEquals(
                applicationCode.getCode(),
                applicationCodeDtoPage.getContent().get(0).getApplicationCode());
        assertEquals(
                applicationCode2.getCode(),
                applicationCodeDtoPage.getContent().get(1).getApplicationCode());
        assertEquals(
                applicationCode3.getCode(),
                applicationCodeDtoPage.getContent().get(2).getApplicationCode());
        assertEquals(
                applicationCode4.getCode(),
                applicationCodeDtoPage.getContent().get(3).getApplicationCode());
        verify(feeService).resolveFeePairs(List.of("REF-1", "REF-2", "REF-3", "REF-4"), todayUk);
    }

    @Test
    void findAll_emptyPage_returnsEmptyContentList() {
        Pageable criteria = Pageable.ofSize(10);
        PageImpl<ApplicationCode> results =
                new PageImpl<>(List.of(), Pageable.ofSize(10).withPage(0), 0);

        when(repository.search(null, null, FIXED_BUSINESS_DATE, criteria)).thenReturn(results);

        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        null, null, FIXED_BUSINESS_DATE, PagingWrapper.of(List.of(), criteria));

        assertNotNull(applicationCodeDtoPage.getContent());
        assertEquals(0, applicationCodeDtoPage.getContent().size());
    }

    @Test
    void findAllByTitle() {
        var applicationCode = createApplicationCodeWithFeeReference("REF-1");
        var applicationCode2 = createApplicationCodeWithFeeReference("REF-2");
        var applicationCode3 = createApplicationCodeWithFeeReference("REF-3");
        var applicationCode4 = createApplicationCodeWithFeeReference("REF-4");

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
        LocalDate todayUk = FIXED_BUSINESS_DATE;
        when(repository.search(null, title, todayUk, criteria)).thenReturn(results);

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();
        var feePairs =
                Map.of(
                        "REF-1", new FeePair(dummyMain, dummyOffset),
                        "REF-2", new FeePair(dummyMain, dummyOffset),
                        "REF-3", new FeePair(dummyMain, dummyOffset),
                        "REF-4", new FeePair(dummyMain, dummyOffset));

        when(feeService.resolveFeePairs(List.of("REF-1", "REF-2", "REF-3", "REF-4"), todayUk))
                .thenReturn(feePairs);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        null, title, null, PagingWrapper.of(List.of(), criteria));

        // make assertion
        assertEquals(4, applicationCodeDtoPage.getTotalElements());
        assertEquals(
                applicationCode.getCode(),
                applicationCodeDtoPage.getContent().get(0).getApplicationCode());
        assertEquals(
                applicationCode2.getCode(),
                applicationCodeDtoPage.getContent().get(1).getApplicationCode());
        assertEquals(
                applicationCode3.getCode(),
                applicationCodeDtoPage.getContent().get(2).getApplicationCode());
        assertEquals(
                applicationCode4.getCode(),
                applicationCodeDtoPage.getContent().get(3).getApplicationCode());
    }

    @Test
    void findAllByDate() {
        var applicationCode = createApplicationCodeWithFeeReference("REF-1");
        var applicationCode2 = createApplicationCodeWithFeeReference("REF-2");
        var applicationCode3 = createApplicationCodeWithFeeReference("REF-3");
        var applicationCode4 = createApplicationCodeWithFeeReference("REF-4");

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

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();
        var feePairs =
                Map.of(
                        "REF-1", new FeePair(dummyMain, dummyOffset),
                        "REF-2", new FeePair(dummyMain, dummyOffset),
                        "REF-3", new FeePair(dummyMain, dummyOffset),
                        "REF-4", new FeePair(dummyMain, dummyOffset));

        String title = "title";
        String code = "code";
        LocalDate effectiveDate = LocalDate.of(2021, Month.JUNE, 15);
        when(repository.search(code, title, effectiveDate, criteria)).thenReturn(results);
        when(feeService.resolveFeePairs(List.of("REF-1", "REF-2", "REF-3", "REF-4"), effectiveDate))
                .thenReturn(feePairs);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        code, title, effectiveDate, PagingWrapper.of(List.of(), criteria));

        // make assertion
        assertEquals(4, applicationCodeDtoPage.getTotalElements());
        assertEquals(
                applicationCode.getCode(),
                applicationCodeDtoPage.getContent().get(0).getApplicationCode());
        assertEquals(
                applicationCode2.getCode(),
                applicationCodeDtoPage.getContent().get(1).getApplicationCode());
        assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    @Test
    void findAllCriteria() {
        var applicationCode = createApplicationCodeWithFeeReference("REF-1");
        var applicationCode2 = createApplicationCodeWithFeeReference("REF-2");
        var applicationCode3 = createApplicationCodeWithFeeReference("REF-3");
        var applicationCode4 = createApplicationCodeWithFeeReference("REF-4");

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
        LocalDate todayUk = FIXED_BUSINESS_DATE;
        when(repository.search(null, null, todayUk, criteria)).thenReturn(results);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        Fee dummyMain = new FeeTestData().someComplete();
        Fee dummyOffset = new FeeTestData().someComplete();
        var feePairs =
                Map.of(
                        "REF-1", new FeePair(dummyMain, dummyOffset),
                        "REF-2", new FeePair(dummyMain, dummyOffset),
                        "REF-3", new FeePair(dummyMain, dummyOffset),
                        "REF-4", new FeePair(dummyMain, dummyOffset));

        when(feeService.resolveFeePairs(List.of("REF-1", "REF-2", "REF-3", "REF-4"), todayUk))
                .thenReturn(feePairs);

        // execute test
        ApplicationCodePage applicationCodeDtoPage =
                applicationCodeService.findAll(
                        null, null, null, PagingWrapper.of(List.of(), criteria.withPage(0)));

        // make assertion
        assertEquals(4, applicationCodeDtoPage.getTotalElements());
        assertEquals(
                applicationCodeDtoPage.getContent().get(0).getApplicationCode(),
                applicationCode.getCode());
        assertEquals(
                applicationCodeDtoPage.getContent().get(1).getApplicationCode(),
                applicationCode2.getCode());
        assertEquals(
                applicationCodeDtoPage.getContent().get(2).getApplicationCode(),
                applicationCode3.getCode());
        assertEquals(
                applicationCodeDtoPage.getContent().get(3).getApplicationCode(),
                applicationCode4.getCode());
    }

    @Test
    void findAllCachesFeePairsByReferenceForThePage() {
        var first = new ApplicationCodeTestData().someComplete();
        var second = new ApplicationCodeTestData().someComplete();
        var third = new ApplicationCodeTestData().someComplete();
        first.setFeeReference("SHARED");
        second.setFeeReference("SHARED");
        third.setFeeReference("UNIQUE");

        var criteria = Pageable.ofSize(10);
        var results =
                new PageImpl<>(List.of(first, second, third), Pageable.ofSize(3).withPage(0), 3);
        var todayUk = FIXED_BUSINESS_DATE;
        var feePairs = new LinkedHashMap<String, FeePair>();
        feePairs.put("SHARED", new FeePair(new FeeTestData().someComplete(), null));
        feePairs.put("UNIQUE", new FeePair(new FeeTestData().someComplete(), null));

        when(repository.search("code", null, todayUk, criteria)).thenReturn(results);
        when(feeService.resolveFeePairs(List.of("SHARED", "UNIQUE"), todayUk)).thenReturn(feePairs);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        applicationCodeService.findAll("code", null, null, PagingWrapper.of(List.of(), criteria));

        verify(feeService).resolveFeePairs(List.of("SHARED", "UNIQUE"), todayUk);
        verify(feeService, never()).resolveFeePair(Mockito.notNull(), eq(todayUk));
    }

    @Test
    void findByCodeWithFeesEmpty() {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();

        GetApplicationCodeValidationSuccess success =
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCode)
                        .build();
        dummyGetApplicationCodeValidator.setSuccess(success);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapper());

        when(feeService.resolveFeePair(Mockito.notNull(), Mockito.notNull()))
                .thenReturn(new FeePair(null, null));

        String code = "code";

        LocalDate localDate = REQUEST_DATE;

        PayloadForGet payloadForGet = PayloadForGet.builder().code(code).date(localDate).build();
        ApplicationCodeGetDetailDto applicationCodeDto =
                applicationCodeService.findByCode(payloadForGet);

        assertEquals(applicationCodeDto.getApplicationCode(), applicationCode.getCode());
        assertEquals(JsonNullable.undefined(), applicationCodeDto.getFeeAmount());
        assertEquals(JsonNullable.undefined(), applicationCodeDto.getOffsiteFeeAmount());
    }

    private ApplicationCodeServiceImpl buildServiceWithListeners(
            List<AuditOperationLifecycleListener> listeners) {
        return new ApplicationCodeServiceImpl(
                repository,
                applicationCodeMapper,
                feeService,
                new AuditOperationServiceImpl(listeners),
                pageMapper,
                fixedClock,
                ukZone,
                dummyGetApplicationCodeValidator);
    }

    private ApplicationCode createApplicationCodeWithFeeReference(String feeReference) {
        var applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setFeeReference(feeReference);
        return applicationCode;
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

        void setSuccess(GetApplicationCodeValidationSuccess success) {
            this.success = success;
        }
    }
}
