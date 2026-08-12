package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkImportWriteAuditMode;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntrySequenceMappingRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkImportServiceTest {
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, Month.JULY, 14);

    @Mock private ApplicationListEntryEntityMapper entryMapper;
    @Mock private ApplicantMapper applicantMapper;
    @Mock private NameAddressRepository nameAddressRepository;
    @Mock private ApplicationListEntryRepository entryRepository;
    @Mock private AppListEntrySequenceMappingRepository sequenceMappingRepository;
    @Mock private AppListEntryFeeStatusRepository feeStatusRepository;
    @Mock private AppListEntryOfficialRepository officialRepository;
    @Mock private AppListEntryFeeRepository entryFeeRepository;
    @Mock private AsyncJobAppListEntryRepository asyncJobEntryRepository;
    @Mock private AuditOperationService auditService;
    @Mock private BusinessDateProvider businessDateProvider;
    @Mock private EntityManager entityManager;

    private BulkImportService service;
    private ApplicationList applicationList;
    private Fee mainFee;

    @BeforeEach
    void setUp() {
        applicationList = new ApplicationList();
        applicationList.setId(100L);
        mainFee = new Fee();
        mainFee.setId(200L);

        service =
                new BulkImportService(
                        entryMapper,
                        applicantMapper,
                        nameAddressRepository,
                        entryRepository,
                        sequenceMappingRepository,
                        feeStatusRepository,
                        officialRepository,
                        entryFeeRepository,
                        asyncJobEntryRepository,
                        auditService,
                        businessDateProvider,
                        Clock.fixed(Instant.parse("2026-07-14T10:00:00Z"), ZoneOffset.UTC),
                        entityManager);

        when(businessDateProvider.currentUkDate()).thenReturn(BUSINESS_DATE);
        when(sequenceMappingRepository.findByAlIdForUpdate(applicationList.getId()))
                .thenReturn(Optional.empty());
        when(entryMapper.toApplicationListEntry(
                        any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new ApplicationListEntry());
        var nextId = new AtomicLong(1);
        when(entryRepository.saveAll(any()))
                .thenAnswer(
                        invocation -> {
                            List<ApplicationListEntry> entries = toList(invocation.getArgument(0));
                            entries.forEach(entry -> entry.setId(nextId.getAndIncrement()));
                            return entries;
                        });
        when(entryRepository.findIdsAndUuidsByIdIn(any()))
                .thenAnswer(
                        invocation -> {
                            Collection<Long> ids = invocation.getArgument(0);
                            return ids.stream().map(this::generatedUuid).toList();
                        });
    }

    @Test
    void givenBulkMode_whenPersistingPage_thenBatchesEntriesAndAssociatedWritesWithoutAudits() {
        UUID jobId = UUID.randomUUID();

        int imported = service.persistPage(jobId, List.of(validatedEntry(), validatedEntry()));

        assertThat(imported).isEqualTo(2);
        var entriesCaptor = ArgumentCaptor.<Iterable<ApplicationListEntry>>captor();
        verify(entryRepository).saveAll(entriesCaptor.capture());
        List<ApplicationListEntry> entries = toList(entriesCaptor.getValue());
        assertThat(entries)
                .extracting(ApplicationListEntry::getSequenceNumber)
                .containsExactly((short) 1, (short) 2);
        verify(entryMapper, times(2))
                .toApplicationListEntry(
                        any(), eq("Wording"), any(), any(), any(), any(), any(), any());
        verify(feeStatusRepository).saveAll(anyList());
        verify(entryFeeRepository).saveAll(anyList());
        var jobsCaptor = ArgumentCaptor.<Iterable<AsyncJobsAppListEntry>>captor();
        verify(asyncJobEntryRepository).saveAll(jobsCaptor.capture());
        List<AsyncJobsAppListEntry> jobEntries = toList(jobsCaptor.getValue());
        assertThat(jobEntries).hasSize(2).allMatch(entry -> jobId.equals(entry.getAsyncJobId()));
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST), any());
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    @Test
    void givenPerEntryMode_whenPersistingPage_thenAuditsEachEntryOnly() {
        ReflectionTestUtils.setField(service, "writeAuditMode", BulkImportWriteAuditMode.PER_ENTRY);

        service.persistPage(UUID.randomUUID(), List.of(validatedEntry(), validatedEntry()));

        verify(auditService, times(2))
                .processAudit(eq(AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST), any());
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_APPLICANT), any());
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_RESPONDENT), any());
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_FEE_ENTRY), any());
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY), any());
        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY), any());
    }

    @Test
    void givenExistingSequenceMapping_whenPersistingPage_thenReservesOneContiguousRange() {
        var mapping =
                AppListEntrySequenceMapping.builder()
                        .alId(applicationList.getId())
                        .aleLastSequence(10)
                        .build();
        when(sequenceMappingRepository.findByAlIdForUpdate(applicationList.getId()))
                .thenReturn(Optional.of(mapping));

        service.persistPage(UUID.randomUUID(), List.of(validatedEntry(), validatedEntry()));

        assertThat(mapping.getAleLastSequence()).isEqualTo(12);
        var entriesCaptor = ArgumentCaptor.<Iterable<ApplicationListEntry>>captor();
        verify(entryRepository).saveAll(entriesCaptor.capture());
        assertThat(toList(entriesCaptor.getValue()))
                .extracting(ApplicationListEntry::getSequenceNumber)
                .containsExactly((short) 11, (short) 12);
    }

    @Test
    void givenBulkMode_whenJobCompletes_thenAuditsSummaryOnce() {
        service.completed(UUID.randomUUID(), UUID.randomUUID(), 25);

        verify(auditService)
                .processAudit(eq(AppListEntryAuditOperation.BULK_IMPORT_APP_ENTRIES), any());
    }

    @Test
    void givenPerEntryMode_whenJobCompletes_thenDoesNotAuditSummary() {
        ReflectionTestUtils.setField(service, "writeAuditMode", BulkImportWriteAuditMode.PER_ENTRY);

        service.completed(UUID.randomUUID(), UUID.randomUUID(), 25);

        verify(auditService, never())
                .processAudit(eq(AppListEntryAuditOperation.BULK_IMPORT_APP_ENTRIES), any());
    }

    @Test
    void givenEmptyPage_whenPersisting_thenDoesNothing() {
        assertThat(service.persistPage(UUID.randomUUID(), List.of())).isZero();

        verify(entryRepository, never()).saveAll(any());
    }

    @Test
    void givenNullRespondent_whenPersistingPage_thenDoesNotCreateRespondent() {
        var validatedEntry = validatedEntry();
        validatedEntry.entry().setRespondent(null);

        service.persistPage(UUID.randomUUID(), List.of(validatedEntry));

        verify(applicantMapper, never()).toRespondent(any());
        verify(nameAddressRepository).saveAll(anyList());
        verify(entryMapper)
                .toApplicationListEntry(
                        eq(validatedEntry.entry()),
                        eq("Wording"),
                        any(),
                        any(),
                        eq(null),
                        any(),
                        any(),
                        any());
    }

    private ValidatedBulkImportEntry validatedEntry() {
        var dto = new EntryCreateDto();
        dto.setWordingFields(List.of());
        dto.setHasOffsiteFee(false);
        var applicationCode = new ApplicationCode();
        applicationCode.setWording("Wording");
        var validation =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with("Wording"))
                        .applicationCode(applicationCode)
                        .applicationList(applicationList)
                        .fee(new FeePair(mainFee, null))
                        .build();
        return new ValidatedBulkImportEntry(2, dto, validation, "Wording");
    }

    private ApplicationListEntryRepository.EntryIdAndUuid generatedUuid(Long id) {
        var projection = mock(ApplicationListEntryRepository.EntryIdAndUuid.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getUuid()).thenReturn(UUID.randomUUID());
        return projection;
    }

    private static <T> List<T> toList(Iterable<T> values) {
        var result = new ArrayList<T>();
        values.forEach(result::add);
        return result;
    }
}
