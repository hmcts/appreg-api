package uk.gov.hmcts.appregister.standardapplicant.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapperImpl;
import uk.gov.hmcts.appregister.standardapplicant.validator.StandardApplicantExistsValidator;

@ExtendWith(MockitoExtension.class)
class StandardApplicantServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-09T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
    private static final LocalDate CURRENT_UK_DATE = LocalDate.of(2026, Month.JUNE, 9);

    @Mock private StandardApplicantRepository repository;

    @Spy
    private DummyStandardApplicantExistsValidator validator =
            new DummyStandardApplicantExistsValidator(repository);

    @Spy
    private List<AuditOperationLifecycleListener> listeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService = new AuditOperationServiceImpl(listeners);

    @Spy
    private StandardApplicantMapperImpl standardApplicantMapper = new StandardApplicantMapperImpl();

    @Mock private Clock clock;

    @Spy private ZoneId ukZone = ZoneId.of("Europe/London");

    @Spy private PageMapper pageMapper = new PageMapper();

    @InjectMocks private StandardApplicationServiceImpl standardApplicantService;

    @BeforeEach
    void before() {
        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());
    }

    @Test
    void testGetAll() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val from = CURRENT_UK_DATE.minusDays(10);
        val to = CURRENT_UK_DATE.plusDays(10);

        val standardApplicant1 = new StandardApplicant();
        standardApplicant1.setApplicantCode("APP001");
        standardApplicant1.setName("John Doe");
        standardApplicant1.setApplicantStartDate(from);
        standardApplicant1.setApplicantEndDate(to);

        val standardApplicant2 = new StandardApplicant();
        standardApplicant2.setApplicantCode("APP002");
        standardApplicant2.setName("Jane Doe");
        standardApplicant2.setApplicantStartDate(from.plusDays(1));
        standardApplicant2.setApplicantEndDate(to.plusDays(1));

        val projection1 = mock(StandardApplicantEnrichedProjection.class);
        val projection2 = mock(StandardApplicantEnrichedProjection.class);
        val code = "APP001";
        val name = "John Doe";
        val addressLine1 = "123 Main Street";
        val pageable = PageRequest.of(0, 2);

        when(projection1.getStandardApplicant()).thenReturn(standardApplicant1);
        when(projection1.getEffectiveName()).thenReturn("John Doe");

        when(projection2.getStandardApplicant()).thenReturn(standardApplicant2);
        when(projection2.getEffectiveName()).thenReturn("Jane Doe");

        val pageImpl = new PageImpl<>(java.util.List.of(projection1, projection2), pageable, 2);

        when(repository.search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(pageable)))
                .thenReturn(pageImpl);

        val wrapper = PagingWrapper.of(List.of(), pageable);

        val standardApplicantPage =
                standardApplicantService.findAll(code, name, addressLine1, from, to, wrapper);
        val firstResult = standardApplicantPage.getContent().getFirst();

        verify(repository)
                .search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(pageable));

        Assertions.assertEquals(2, standardApplicantPage.getTotalElements());
        Assertions.assertEquals(standardApplicant1.getApplicantCode(), firstResult.getCode());
        Assertions.assertEquals(
                standardApplicant1.getName(),
                firstResult.getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                standardApplicant1.getApplicantStartDate(), firstResult.getStartDate());
        Assertions.assertEquals(
                standardApplicant1.getApplicantEndDate(), firstResult.getEndDate().get());

        val secondResult = standardApplicantPage.getContent().get(1);
        Assertions.assertEquals(standardApplicant2.getApplicantCode(), secondResult.getCode());
        Assertions.assertEquals(
                standardApplicant2.getName(),
                secondResult.getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                standardApplicant2.getApplicantStartDate(), secondResult.getStartDate());
        Assertions.assertEquals(
                standardApplicant2.getApplicantEndDate(), secondResult.getEndDate().get());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(StandardApplicantOperation.GET_STANDARD_APPLICANTS),
                        notNull());
    }

    @Test
    void testGetByCode() {
        val code = "APP001";

        val standardApplicantGetDetailDto = standardApplicantService.findByCode(code);

        Assertions.assertEquals(standardApplicantGetDetailDto.getCode(), code);

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE),
                        notNull());
    }

    @Test
    void testGetByCode_auditsRequestedLookupCriteria() {
        val code = "APP001";
        val standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantCode(code);
        standardApplicant.setName("John Doe");
        standardApplicant.setApplicantStartDate(LocalDate.of(2020, Month.JANUARY, 1));
        validator.setSuccess(standardApplicant);

        val listener = new CapturingAuditListener();
        val localService =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(List.of(listener)),
                        new ApplicantMapperImpl());

        val actual = localService.findByCode(code);

        Assertions.assertEquals(code, actual.getCode());
        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertNotSame(standardApplicant, audited);
        Assertions.assertEquals(code, audited.getApplicantCode());
        Assertions.assertNull(audited.getApplicantStartDate());
    }

    @Test
    void testGetAll_auditsRequestedSearchCriteria() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val code = "APP001";
        val name = "John Doe";
        val from = LocalDate.of(2026, Month.APRIL, 1);
        val to = LocalDate.of(2026, Month.DECEMBER, 31);

        val applicant = new StandardApplicant();
        applicant.setApplicantCode(code);
        applicant.setName(name);
        applicant.setApplicantStartDate(from);
        applicant.setApplicantEndDate(to);
        val addressLine1 = "123 Main Street";
        val pageable = PageRequest.of(0, 2);
        val projection = mock(StandardApplicantEnrichedProjection.class);
        when(projection.getStandardApplicant()).thenReturn(applicant);
        when(projection.getEffectiveName()).thenReturn(name);
        when(repository.search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        val listener = new CapturingAuditListener();
        val localService =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(List.of(listener)),
                        new ApplicantMapperImpl());

        // Execute the search with every currently in-scope DB-backed filter populated so the
        // resulting audit surrogate can be asserted directly.
        localService.findAll(
                code, name, addressLine1, from, to, PagingWrapper.of(List.of(), pageable));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(code, audited.getApplicantCode());
        Assertions.assertEquals(name, audited.getName());
        Assertions.assertEquals(addressLine1, audited.getAddressLine1());
        Assertions.assertEquals(from, audited.getApplicantStartDate());
        Assertions.assertEquals(to, audited.getApplicantEndDate());
    }

    @Test
    void testGetAll_normalisesReversedDateRangeBeforeSearchAndAudit() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val code = "APP001";
        val name = "John Doe";
        val requestedFrom = LocalDate.of(2026, Month.DECEMBER, 31);
        val requestedTo = LocalDate.of(2026, Month.APRIL, 1);

        val applicant = new StandardApplicant();
        applicant.setApplicantCode(code);
        applicant.setName(name);
        applicant.setApplicantStartDate(requestedTo);
        applicant.setApplicantEndDate(requestedFrom);
        val addressLine1 = "123 Main Street";
        val pageable = PageRequest.of(0, 2);
        val projection = mock(StandardApplicantEnrichedProjection.class);
        when(projection.getStandardApplicant()).thenReturn(applicant);
        when(projection.getEffectiveName()).thenReturn(name);
        when(repository.search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(requestedTo),
                        eq(requestedFrom),
                        isNotNull(),
                        eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        val listener = new CapturingAuditListener();
        val localService =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(List.of(listener)),
                        new ApplicantMapperImpl());

        localService.findAll(
                code,
                name,
                addressLine1,
                requestedFrom,
                requestedTo,
                PagingWrapper.of(List.of(), pageable));

        verify(repository)
                .search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(requestedTo),
                        eq(requestedFrom),
                        isNotNull(),
                        eq(pageable));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(requestedTo, audited.getApplicantStartDate());
        Assertions.assertEquals(requestedFrom, audited.getApplicantEndDate());
    }

    @Test
    void testUpsertStandardApplicant_insert() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);
        when(repository.findStandardApplicantByCode("APP001", CURRENT_UK_DATE))
                .thenReturn(List.of());

        val standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantCode("APP001");
        standardApplicant.setName("John Doe");
        standardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
        standardApplicant.setApplicantEndDate(CURRENT_UK_DATE.plusDays(1));

        val listener = new CapturingAuditListener();
        val serviceImpl =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        new ApplicantMapperImpl());

        serviceImpl.upsertStandardApplicant(standardApplicant);

        verify(repository, times(1)).findStandardApplicantByCode("APP001", CURRENT_UK_DATE);
        verify(repository, times(1)).saveAndFlush(any(StandardApplicant.class));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(standardApplicant.getApplicantCode(), audited.getApplicantCode());
        Assertions.assertEquals(standardApplicant.getName(), audited.getName());
        Assertions.assertEquals(standardApplicant.getAddressLine1(), audited.getAddressLine1());
        Assertions.assertEquals(
                standardApplicant.getApplicantStartDate(), audited.getApplicantStartDate());
        Assertions.assertEquals(
                standardApplicant.getApplicantEndDate(), audited.getApplicantEndDate());
    }

    @Test
    void testUpsertStandardApplicant_update() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val existingStandardApplicant = new StandardApplicant();
        existingStandardApplicant.setId(67L);
        existingStandardApplicant.setApplicantCode("APP001");
        existingStandardApplicant.setName("John Doe");
        existingStandardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
        existingStandardApplicant.setApplicantEndDate(null);
        when(repository.findStandardApplicantByCode("APP001", CURRENT_UK_DATE))
                .thenReturn(List.of(existingStandardApplicant));

        val standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantCode("APP001");
        standardApplicant.setName("John Deer");
        standardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
        standardApplicant.setApplicantEndDate(null);

        val listener = new CapturingAuditListener();
        val serviceImpl =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        new ApplicantMapperImpl());

        serviceImpl.upsertStandardApplicant(standardApplicant);

        verify(repository, times(1))
                .findStandardApplicantByCode(
                        existingStandardApplicant.getApplicantCode(), CURRENT_UK_DATE);
        verify(repository, times(1)).saveAndFlush(any(StandardApplicant.class));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(67, audited.getId());
        Assertions.assertEquals(standardApplicant.getApplicantCode(), audited.getApplicantCode());
        Assertions.assertEquals(standardApplicant.getName(), audited.getName());
        Assertions.assertEquals(standardApplicant.getAddressLine1(), audited.getAddressLine1());
        Assertions.assertEquals(
                standardApplicant.getApplicantStartDate(), audited.getApplicantStartDate());
        Assertions.assertEquals(
                standardApplicant.getApplicantEndDate(), audited.getApplicantEndDate());
    }

    @Test
    void testUpsertStandardApplicant_update_endDateToNull() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val existingStandardApplicant = new StandardApplicant();
        existingStandardApplicant.setId(67L);
        existingStandardApplicant.setApplicantCode("APP001");
        existingStandardApplicant.setName("John Doe");
        existingStandardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
        existingStandardApplicant.setApplicantEndDate(CURRENT_UK_DATE.plusDays(1));
        when(repository.findStandardApplicantByCode("APP001", CURRENT_UK_DATE))
            .thenReturn(List.of(existingStandardApplicant));

        val standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantCode("APP001");
        standardApplicant.setName("John Deer");
        standardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
        standardApplicant.setApplicantEndDate(null);

        val listener = new CapturingAuditListener();
        val serviceImpl =
            new StandardApplicationServiceImpl(
                repository,
                standardApplicantMapper,
                clock,
                ukZone,
                pageMapper,
                validator,
                new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                List.of(listener),
                new ApplicantMapperImpl());

        serviceImpl.upsertStandardApplicant(standardApplicant);

        verify(repository, times(1))
            .findStandardApplicantByCode(
                existingStandardApplicant.getApplicantCode(), CURRENT_UK_DATE);
        verify(repository, times(1)).saveAndFlush(any(StandardApplicant.class));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(67, audited.getId());
        Assertions.assertEquals(standardApplicant.getApplicantCode(), audited.getApplicantCode());
        Assertions.assertEquals(standardApplicant.getName(), audited.getName());
        Assertions.assertEquals(standardApplicant.getAddressLine1(), audited.getAddressLine1());
        Assertions.assertEquals(
            standardApplicant.getApplicantStartDate(), audited.getApplicantStartDate());
        Assertions.assertEquals(
            standardApplicant.getApplicantEndDate(), audited.getApplicantEndDate());
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
    static class DummyStandardApplicantExistsValidator extends StandardApplicantExistsValidator {
        private StandardApplicant success;

        public DummyStandardApplicantExistsValidator(StandardApplicantRepository repository) {
            super(repository, FIXED_CLOCK, ZoneId.of("Europe/London"));
        }

        @Override
        public <R> R validate(
                String code, BiFunction<String, StandardApplicant, R> createApplicationSupplier) {
            return createApplicationSupplier.apply(
                    code, success != null ? success : defaultApplicant());
        }

        private StandardApplicant defaultApplicant() {
            val standardApplicant = new StandardApplicant();
            standardApplicant.setApplicantCode("APP001");
            standardApplicant.setName("John Doe");
            standardApplicant.setApplicantStartDate(CURRENT_UK_DATE);
            standardApplicant.setApplicantEndDate(CURRENT_UK_DATE.plusDays(1));
            return standardApplicant;
        }
    }
}
