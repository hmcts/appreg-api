package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkImportAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkImportWriteAuditMode;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.audit.annotation.NestedAudit;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntrySequenceMappingRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

/**
 * Persists already-validated bulk-import pages without invoking the user-facing create path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private static final String APPLICATION_TEXT_COLUMNS = "APPLICATION_TEXT";
    private static final Set<ErrorCodeEnum> CLIENT_SAFE_WORDING_ERRORS =
            Set.of(CommonAppError.WORDING_DATA_TYPE_FAILURE, CommonAppError.WORDING_LENGTH_FAILURE);

    @Value("${appreg.bulk-import.write-audit-mode:BULK}")
    private BulkImportWriteAuditMode writeAuditMode = BulkImportWriteAuditMode.BULK;

    private final ApplicationListEntryEntityMapper entryMapper;
    private final ApplicantMapper applicantMapper;
    private final NameAddressRepository nameAddressRepository;
    private final ApplicationListEntryRepository entryRepository;
    private final AppListEntrySequenceMappingRepository sequenceMappingRepository;
    private final AppListEntryFeeStatusRepository feeStatusRepository;
    private final AppListEntryOfficialRepository officialRepository;
    private final AppListEntryFeeRepository entryFeeRepository;
    private final AsyncJobAppListEntryRepository asyncJobEntryRepository;
    private final AuditOperationService auditService;
    private final BusinessDateProvider businessDateProvider;
    private final Clock clock;
    private final EntityManager entityManager;

    /**
     * Persists one reader page using Hibernate's configured insert batching.
     *
     * <p>The persistence context is flushed and cleared per page, which bounds memory for large
     * files and avoids the per-entry refresh and ETag queries used by interactive creates.
     *
     * @return the number of entries persisted
     */
    @Transactional
    @NestedAudit
    public int persistPage(UUID jobId, List<ValidatedBulkImportEntry> validatedEntries) {
        if (validatedEntries.isEmpty()) {
            return 0;
        }

        var firstSequence =
                reserveSequences(
                        validatedEntries.getFirst().validationResult().getApplicationList().getId(),
                        validatedEntries.size());
        var names = new ArrayList<NameAddress>();
        var entries = new ArrayList<ApplicationListEntry>(validatedEntries.size());

        for (var index = 0; index < validatedEntries.size(); index++) {
            var validatedEntry = validatedEntries.get(index);
            var entry = createEntry(validatedEntry, names, firstSequence + index);
            entries.add(entry);
        }

        nameAddressRepository.saveAll(names);
        entryRepository.saveAll(entries);

        var feeStatuses = new ArrayList<AppListEntryFeeStatus>();
        var officials = new ArrayList<AppListEntryOfficial>();
        var entryFees = new ArrayList<AppListEntryFeeId>();
        for (var index = 0; index < validatedEntries.size(); index++) {
            addAssociatedWrites(
                    validatedEntries.get(index),
                    entries.get(index),
                    feeStatuses,
                    officials,
                    entryFees);
        }

        feeStatusRepository.saveAll(feeStatuses);
        officialRepository.saveAll(officials);
        entryFeeRepository.saveAll(entryFees);

        entryRepository.flush();
        populateGeneratedUuids(entries);
        if (jobId != null) {
            asyncJobEntryRepository.saveAll(
                    entries.stream()
                            .map(
                                    entry ->
                                            AsyncJobsAppListEntry.builder()
                                                    .asyncJobId(jobId)
                                                    .appListEntryId(entry.getUuid())
                                                    .build())
                            .toList());
        }

        if (writeAuditMode == BulkImportWriteAuditMode.PER_ENTRY) {
            entries.forEach(this::auditEntry);
        }

        entityManager.flush();
        entityManager.clear();
        log.debug("Persisted {} bulk-import entries for job {}", entries.size(), jobId);
        return entries.size();
    }

    /** Emits the single successful-job audit used in BULK mode. */
    @Transactional
    @NestedAudit
    public void completed(UUID listId, UUID jobId, int importedEntryCount) {
        if (writeAuditMode != BulkImportWriteAuditMode.BULK) {
            return;
        }

        var audit = new BulkImportAudit(listId, jobId, importedEntryCount);
        auditService.processAudit(
                AppListEntryAuditOperation.BULK_IMPORT_APP_ENTRIES,
                ignored -> Optional.of(new AuditableResult<>(null, audit)));
    }

    private ApplicationListEntry createEntry(
            ValidatedBulkImportEntry validatedEntry, List<NameAddress> names, int sequenceNumber) {
        EntryCreateDto dto = validatedEntry.entry();
        var validation = validatedEntry.validationResult();
        var applicant = createApplicant(dto, names);
        var respondent = createRespondent(dto, names);
        var entry =
                entryMapper.toApplicationListEntry(
                        dto,
                        substituteWording(validatedEntry),
                        validation.getSa(),
                        applicant,
                        respondent,
                        validation.getApplicationCode(),
                        validation.getApplicationList(),
                        YesOrNo.YES);
        if (sequenceNumber > Short.MAX_VALUE) {
            throw new IllegalStateException(
                    "Application-list entry sequence exceeds "
                            + Short.MAX_VALUE
                            + ": "
                            + sequenceNumber);
        }
        entry.setSequenceNumber((short) sequenceNumber);
        return entry;
    }

    private static String substituteWording(ValidatedBulkImportEntry validatedEntry) {
        try {
            return validatedEntry
                    .validationResult()
                    .getWordingSentence()
                    .substitute(validatedEntry.entry().getWordingFields())
                    .getSubstitutedString();
        } catch (AppRegistryException exception) {
            if (!CLIENT_SAFE_WORDING_ERRORS.contains(exception.getCode())) {
                throw exception;
            }
            throw new BulkUploadValidationException(
                    validatedEntry.rowNumber(),
                    APPLICATION_TEXT_COLUMNS,
                    exception.getMessage(),
                    exception);
        }
    }

    private NameAddress createApplicant(EntryCreateDto dto, List<NameAddress> names) {
        if (dto.getApplicant() == null
                || (dto.getApplicant().getOrganisation() == null
                        && dto.getApplicant().getPerson() == null)) {
            return null;
        }
        var applicant = applicantMapper.toApplicant(dto.getApplicant());
        names.add(applicant);
        return applicant;
    }

    private NameAddress createRespondent(EntryCreateDto dto, List<NameAddress> names) {
        if (dto.getRespondent() == null) {
            return null;
        }
        var respondent = applicantMapper.toRespondent(dto.getRespondent());
        names.add(respondent);
        return respondent;
    }

    private void addAssociatedWrites(
            ValidatedBulkImportEntry validatedEntry,
            ApplicationListEntry entry,
            List<AppListEntryFeeStatus> feeStatuses,
            List<AppListEntryOfficial> officials,
            List<AppListEntryFeeId> entryFees) {
        var dto = validatedEntry.entry();
        var validation = validatedEntry.validationResult();

        if (validation.getFee() != null && validation.getFee().mainFee() != null) {
            feeStatuses.add(createInitialFeeStatus(entry));
            entryFees.add(createEntryFee(entry, validation.getFee().mainFee().getId()));
        }
        if (validation.getFee() != null
                && validation.getFee().offsiteFee() != null
                && Boolean.TRUE.equals(dto.getHasOffsiteFee())) {
            entryFees.add(createEntryFee(entry, validation.getFee().offsiteFee().getId()));
        }
        if (dto.getOfficials() != null) {
            dto.getOfficials().stream()
                    .map(official -> entryMapper.toOfficial(official, entry))
                    .forEach(officials::add);
        }
    }

    private AppListEntryFeeStatus createInitialFeeStatus(ApplicationListEntry entry) {
        var feeStatus = new AppListEntryFeeStatus();
        feeStatus.setAppListEntry(entry);
        feeStatus.setAlefsFeeStatus(FeeStatusType.DUE);
        feeStatus.setAlefsFeeStatusDate(businessDateProvider.currentUkDate());
        feeStatus.setAlefsStatusCreationDate(OffsetDateTime.now(clock));
        return feeStatus;
    }

    private AppListEntryFeeId createEntryFee(ApplicationListEntry entry, Long feeId) {
        var entryFee = new AppListEntryFeeId();
        entryFee.setAppListEntryId(entry.getId());
        entryFee.setFeeId(feeId);
        return entryFee;
    }

    private int reserveSequences(Long applicationListId, int count) {
        var mapping = sequenceMappingRepository.findByAlIdForUpdate(applicationListId).orElse(null);
        if (mapping == null) {
            sequenceMappingRepository.save(
                    AppListEntrySequenceMapping.builder()
                            .alId(applicationListId)
                            .aleLastSequence(count)
                            .build());
            return 1;
        }

        var firstSequence = mapping.getAleLastSequence() + 1;
        mapping.setAleLastSequence(mapping.getAleLastSequence() + count);
        return firstSequence;
    }

    private void populateGeneratedUuids(List<ApplicationListEntry> entries) {
        var entriesById =
                entries.stream()
                        .collect(Collectors.toMap(ApplicationListEntry::getId, entry -> entry));
        entryRepository
                .findIdsAndUuidsByIdIn(entriesById.keySet())
                .forEach(
                        generated ->
                                entriesById.get(generated.getId()).setUuid(generated.getUuid()));

        if (entries.stream().anyMatch(entry -> entry.getUuid() == null)) {
            throw new IllegalStateException(
                    "Database did not generate every bulk-import entry UUID");
        }
    }

    private void auditEntry(ApplicationListEntry entry) {
        auditService.processAudit(
                AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST,
                ignored -> Optional.of(new AuditableResult<>(null, entry)));
    }
}
