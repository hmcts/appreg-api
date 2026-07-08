package uk.gov.hmcts.appregister.standardapplicant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.CsvParser.parseCsv;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
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
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant_;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapperImpl;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;
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

    //    @Spy private CsvWriter<StandardApplicantCsvRow> csvWriter;

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
    void testGetAll_emptyPage_returnsEmptyContentList() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val pageable = PageRequest.of(0, 2);
        val wrapper = PagingWrapper.of(List.of(), pageable);

        when(repository.search(
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        isNotNull(),
                        eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        val standardApplicantPage =
                standardApplicantService.findAll(null, null, null, null, null, wrapper);

        Assertions.assertNotNull(standardApplicantPage.getContent());
        Assertions.assertTrue(standardApplicantPage.getContent().isEmpty());
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
    void print_whenNoApplicantsMatch_returnsEmptyPrintPayload() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val code = "NOPE";
        val name = "Missing";
        val addressLine1 = "High Street";
        val from = LocalDate.of(2026, Month.APRIL, 1);
        val to = LocalDate.of(2026, Month.DECEMBER, 31);
        val requestPageable = PageRequest.of(0, 20);

        val stableSort =
                requestPageable.getSort().and(Sort.by(Sort.Direction.ASC, StandardApplicant_.ID));
        val printPageable = PageRequest.of(0, 1000, stableSort);

        when(repository.search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(printPageable)))
                .thenReturn(new PageImpl<>(List.of(), printPageable, 0));

        val result =
                standardApplicantService.print(
                        code,
                        name,
                        addressLine1,
                        from,
                        to,
                        PagingWrapper.of(List.of(), requestPageable));

        Assertions.assertEquals(0, result.getRecordCount());
        Assertions.assertTrue(result.getApplicants().isEmpty());
        Assertions.assertEquals(addressLine1, result.getSearchCriteria().getAddressLine1().get());
        Assertions.assertEquals(from, result.getSearchCriteria().getFrom().get());
        Assertions.assertEquals(to, result.getSearchCriteria().getTo().get());
    }

    @Test
    void print_whenResultLimitExceeded_throwsBeforeFetchingNextPage() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ukZone)).thenReturn(clock);

        val code = "APP";
        val name = "Applicant";
        val from = LocalDate.of(2026, Month.APRIL, 1);
        val to = LocalDate.of(2026, Month.DECEMBER, 31);
        val requestPageable = PageRequest.of(0, 20);

        val stableSort =
                requestPageable.getSort().and(Sort.by(Sort.Direction.ASC, StandardApplicant_.ID));
        val firstPrintPageable = PageRequest.of(0, 1000, stableSort);
        val secondPrintPageable = PageRequest.of(1, 1000, stableSort);

        val firstPageRows =
                IntStream.range(0, 1000)
                        .mapToObj(index -> projection("APP%04d".formatted(index), name, from, to))
                        .toList();

        when(repository.search(
                        eq(code),
                        eq(name),
                        isNull(),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(firstPrintPageable)))
                .thenReturn(new PageImpl<>(firstPageRows, firstPrintPageable, 1001));

        assertThatThrownBy(
                        () ->
                                standardApplicantService.print(
                                        code,
                                        name,
                                        null,
                                        from,
                                        to,
                                        PagingWrapper.of(List.of(), requestPageable)))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("exceeds 1000 rows");

        verify(repository, never())
                .search(
                        eq(code),
                        eq(name),
                        isNull(),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(secondPrintPageable));
    }

    private static StandardApplicantEnrichedProjection projection(
            String code, String name, LocalDate from, LocalDate to) {
        val applicant = new StandardApplicant();
        applicant.setApplicantCode(code);
        applicant.setName(name);
        applicant.setApplicantStartDate(from);
        applicant.setApplicantEndDate(to);

        return new StandardApplicantEnrichedProjection() {
            @Override
            public StandardApplicant getStandardApplicant() {
                return applicant;
            }

            @Override
            public String getEffectiveName() {
                return name;
            }
        };
    }

    @Test
    void testExportToCsv_codeFilterSuccess() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        val sa = new StandardApplicant();
        sa.setApplicantCode("APP001");
        sa.setName("Test Org");
        sa.setApplicantStartDate(LocalDate.now(clock));
        sa.setApplicantEndDate(null);
        sa.setPostcode("AB12 3CD");
        sa.setAddressLine1("123 Test Street");
        sa.setAddressLine2("Test Area");
        sa.setAddressLine3("Test City");
        sa.setAddressLine4("Test County");
        sa.setEmailAddress("test@test.com");
        sa.setTelephoneNumber("0123456789");
        sa.setMobileNumber("0987654321");
        sa.setApplicantTitle("Mr");
        sa.setApplicantSurname("Smith");
        sa.setApplicantForename1("John");

        when(repository.findByCodeAndName(eq(sa.getApplicantCode()), any()))
                .thenReturn(List.of(sa));
        String csv = standardApplicantService.generateCsv(sa.getApplicantCode(), null);
        Assertions.assertNotNull(csv);
        Assertions.assertEquals(
                StandardApplicantCsvRow.Header, List.of(csv.split("\n")[0].split("\\|")));
        List<StandardApplicantCsvRow> rows = parseCsv(csv);
        for (int i = 1; i < rows.size(); i++) {
            dataComparison(rows.get(i), sa);
        }
    }

    @Test
    void testExportToCsv_nameFilterSuccess() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        val sa = new StandardApplicant();
        sa.setApplicantCode("APP002");
        sa.setName("Another Org");
        sa.setApplicantStartDate(LocalDate.now(clock));
        sa.setApplicantEndDate(null);
        sa.setPostcode("XY98 7ZT");
        sa.setAddressLine1("456 Another Street");
        sa.setAddressLine2("Another Area");
        sa.setAddressLine3("Another City");
        sa.setAddressLine4("Another County");
        sa.setEmailAddress("test@test.com");
        sa.setTelephoneNumber("0123456789");
        sa.setMobileNumber("0987654321");

        when(repository.findByCodeAndName(any(), eq(sa.getName()))).thenReturn(List.of(sa));
        String csv = standardApplicantService.generateCsv(null, sa.getName());
        Assertions.assertNotNull(csv);
        Assertions.assertEquals(
                StandardApplicantCsvRow.Header, List.of(csv.split("\n")[0].split("\\|")));
        List<StandardApplicantCsvRow> rows = parseCsv(csv);
        for (int i = 1; i < rows.size(); i++) {
            dataComparison(rows.get(i), sa);
        }
    }

    @Test
    void testExportToCsv_codeAndNameFilterFailure() {
        val exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> standardApplicantService.generateCsv("APP001", "Test Org"));
        Assertions.assertEquals(
                StandardApplicantCodeError.CODE_AND_NAME_EXCLUSION_VIOLATION, exception.getCode());
    }

    @Test
    void testExportToCsv_noResultsFoundFailure() {
        when(repository.findByCodeAndName(any(), any())).thenReturn(Collections.emptyList());
        val exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> standardApplicantService.generateCsv("APP001", null));
        Assertions.assertEquals(
                StandardApplicantCodeError.NO_RESULTS_FOUND_FOR_CSV_GENERATION,
                exception.getCode());
    }

    private void dataComparison(StandardApplicantCsvRow row, StandardApplicant expected) {
        Assertions.assertEquals(row.getApplicantCode(), expected.getApplicantCode());
        Assertions.assertEquals(
                row.getName(), expected.getName() == null ? "" : expected.getName());
        Assertions.assertEquals(
                row.getApplicantStartDate(), expected.getApplicantStartDate().toString());
        Assertions.assertEquals(
                row.getApplicantEndDate(),
                expected.getApplicantEndDate() == null
                        ? ""
                        : expected.getApplicantEndDate().toString());
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
