package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryServiceImpl implements ApplicationEntryService {

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final PageMapper pageMapper;

    private final CreateApplicationEntryValidator createApplicationEntryValidator;

    // Services
    private final MatchService matchService;

    // Audit
    private final AuditOperationService auditService;

    private final AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;
    private final NameAddressRepository nameAddressRepository;
    private final AppListEntryOfficialRepository appListEntryOfficialRepository;
    private final AppListEntryFeeRepository appListEntryFeeRepository;

    private final ApplicationListEntryMapper applicationListEntryMapStructMapper;
    private final ApplicantMapper applicantMapper;

    private final ApplicationListEntryEntityMapper applicationListEntryEntityMapper;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    // Infrastructure
    private final EntityManager entityManager;

    @Override
    public EntryPage search(EntryGetFilterDto filterDto, Pageable pageable) {
        Status status = applicationListEntryMapStructMapper.toStatus(filterDto.getStatus());

        log.debug(
                "Started: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                applicationListEntryRepository.searchForGetSummary(
                        filterDto.getDate() != null,
                        filterDto.getDate(),
                        filterDto.getCourtCode(),
                        filterDto.getOtherLocationDescription(),
                        filterDto.getCjaCode(),
                        filterDto.getApplicantOrganisation(),
                        filterDto.getApplicantSurname(),
                        filterDto.getStandardApplicantCode(),
                        status,
                        filterDto.getRespondentOrganisation(),
                        filterDto.getRespondentSurname(),
                        filterDto.getRespondentPostcode(),
                        filterDto.getAccountReference(),
                        pageable);

        // breaks name into individual and/or organisation parts
        EntryPage newPage = new EntryPage();
        pageMapper.toPage(resultPage, newPage);

        // Map each entity to a summary DTO and add to the page content
        resultPage.forEach(
                entry -> {
                    newPage.addContentItem(
                            applicationListEntryMapStructMapper.toEntrySummary(entry));
                });

        log.debug(
                "Finished: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);
        return newPage;
    }

    @Override
    @Transactional
    public MatchResponse<EntryGetDetailDto> createEntry(
            PayloadForCreate<EntryCreateDto> entryCreateDto) {
        log.debug("Started: Create Application Entry: {}", entryCreateDto);

        // creates the entity and return the etag for matching
        log.debug("Started: Create Application Entry: {}", entryCreateDto);
        log.debug("Creating application entry inside list {}", entryCreateDto.getId());

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                createApplicationEntryValidator.validate(
                        entryCreateDto,
                        (dto, success) -> {
                            return auditService.processAudit(
                                    AppListEntryAuditOperation.CREATE_APP_LIST_ENTRY,
                                    req -> {
                                        NameAddress applicantToSave =
                                                createApplicant(entryCreateDto);

                                        NameAddress respondentToSave =
                                                createRespondent(entryCreateDto);

                                        // save the list
                                        ApplicationListEntry listEntryEntity =
                                                applicationListEntryEntityMapper
                                                        .toApplicationListEntry(
                                                                entryCreateDto.getData(),
                                                                success.getWordingSentence()
                                                                        .substitute(
                                                                                entryCreateDto
                                                                                        .getData()
                                                                                        .getWordingFields()),
                                                                success.getSa(),
                                                                applicantToSave,
                                                                respondentToSave,
                                                                success.getApplicationCode(),
                                                                success.getApplicationList());

                                        listEntryEntity =
                                                refreshEntity(
                                                        applicationListEntryRepository.save(
                                                                listEntryEntity));
                                        log.debug(
                                                "Created application entry with id: {}",
                                                listEntryEntity.getId());

                                        List<AppListEntryFeeStatus> statusList =
                                                createFeeStatus(listEntryEntity, entryCreateDto);

                                        List<AppListEntryOfficial> officialList =
                                                createOfficial(listEntryEntity, entryCreateDto);

                                        createFees(success, listEntryEntity, entryCreateDto);

                                        EntryGetDetailDto entryGetDetailDto =
                                                applicationListEntryMapStructMapper
                                                        .toEntryGetDetailDto(
                                                                listEntryEntity,
                                                                statusList,
                                                                success.getFee(),
                                                                officialList,
                                                                success.getSa());
                                        entryGetDetailDto.setHasOffsiteFee(
                                                entryCreateDto.getData().getHasOffsiteFee());

                                        return Optional.of(
                                                new AuditableResult<>(
                                                        MatchResponse.of(
                                                                entryGetDetailDto,
                                                                getKeyablesForCreateUpdateEtag(
                                                                        listEntryEntity)),
                                                        listEntryEntity));
                                    });
                        });

        log.debug("Finish: Create Application Entry: {}", entryCreateDto);

        return getDetailDto;
    }

    /**
     * creates the fees for the entry.
     *
     * @param success The successful validation result
     * @param listEntryEntity The entry entity
     * @param entryCreateDto The create payload containing the fees
     */
    private void createFees(
            CreateApplicationEntryValidationSuccess success,
            ApplicationListEntry listEntryEntity,
            PayloadForCreate<EntryCreateDto> entryCreateDto) {
        if (success.getFee() != null) {
            // save and audit
            auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                    req -> {
                        // create the link between the entry and the
                        // fees
                        AppListEntryFeeId appListEntryFeeId = new AppListEntryFeeId();
                        appListEntryFeeId.setAppListEntryId(listEntryEntity.getId());
                        appListEntryFeeId.setFeeId(success.getFee().getId());

                        log.debug(
                                "Created Fee: {} to Entry: {} mapping: {}",
                                appListEntryFeeId.getFeeId(),
                                appListEntryFeeId.getAppListEntryId());

                        return Optional.of(
                                new AuditableResult<>(
                                        null, appListEntryFeeRepository.save(appListEntryFeeId)));
                    });
        }
    }

    /**
     * create all officials for the entry.
     *
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the officials
     * @return The application list entry officials that were created
     */
    private List<AppListEntryOfficial> createOfficial(
            ApplicationListEntry listEntryEntity, PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryOfficial> officialList = new ArrayList<>();
        if (entryCreateDto.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official : entryCreateDto.getData().getOfficials()) {

                // save and audit
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY,
                        req -> {
                            AppListEntryOfficial newOfficialEntity =
                                    appListEntryOfficialRepository.save(
                                            applicationListEntryEntityMapper.toOfficial(
                                                    official, listEntryEntity));

                            log.debug(
                                    "Official created and mapped to application entry with id: {}",
                                    newOfficialEntity.getId());
                            officialList.add(newOfficialEntity);

                            return Optional.of(new AuditableResult<>(null, newOfficialEntity));
                        });
            }
        }
        return officialList;
    }

    /**
     * create all fee statuses and map them to the entry.
     *
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the officials
     * @return The application fees that were created
     */
    private List<AppListEntryFeeStatus> createFeeStatus(
            ApplicationListEntry listEntryEntity, PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryFeeStatus> statusList = new ArrayList<>();

        if (entryCreateDto.getData().getFeeStatuses() != null) {
            // create the fee statuses and map to entry
            for (FeeStatus feeStatus : entryCreateDto.getData().getFeeStatuses()) {

                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                        req -> {
                            AppListEntryFeeStatus createdAppListStatus =
                                    appListEntryFeeStatusRepository.save(
                                            applicationListEntryEntityMapper.toFeeStatus(
                                                    feeStatus, listEntryEntity));
                            statusList.add(createdAppListStatus);
                            log.debug(
                                    "Fee status created and mapped to application "
                                            + "entry with id: {}",
                                    createdAppListStatus.getId());
                            return Optional.of(new AuditableResult<>(null, createdAppListStatus));
                        });
            }
        }

        return statusList;
    }

    /**
     * creates the applicant for the entry.
     *
     * @param entryCreateDto The applicant data to create
     * @return The created applicant
     */
    private NameAddress createApplicant(PayloadForCreate<EntryCreateDto> entryCreateDto) {
        // save the applicant
        NameAddress applicantToSave = null;
        if (entryCreateDto.getData().getApplicant() != null
                && (entryCreateDto.getData().getApplicant().getOrganisation() != null
                        || entryCreateDto.getData().getApplicant().getPerson() != null)) {

            applicantToSave =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_APPLICANT,
                            req -> {
                                NameAddress applicantToAdded =
                                        applicantMapper.toApplicant(
                                                entryCreateDto.getData().getApplicant());
                                nameAddressRepository.save(applicantToAdded);
                                log.debug(
                                        "Created applicant with id: {}", applicantToAdded.getId());

                                return Optional.of(
                                        new AuditableResult<>(applicantToAdded, applicantToAdded));
                            });
        }
        return applicantToSave;
    }

    /**
     * creates the respondent for the application entry.
     *
     * @param entryCreateDto The applicant data to create
     * @return The created respondent
     */
    private NameAddress createRespondent(PayloadForCreate<EntryCreateDto> entryCreateDto) {
        // save the respondent
        NameAddress respondentToSave = null;
        if (entryCreateDto.getData().getRespondent() != null) {
            respondentToSave =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_RESPONDENT,
                            req -> {
                                NameAddress respondentToAdded =
                                        nameAddressRepository.save(
                                                applicantMapper.toRespondent(
                                                        entryCreateDto.getData().getRespondent()));
                                log.debug(
                                        "Created respondent with id: {}",
                                        respondentToAdded.getId());

                                return Optional.of(
                                        new AuditableResult<>(
                                                respondentToAdded, respondentToAdded));
                            });
        }

        return respondentToSave;
    }

    /**
     * Reloads the entity so DB-generated fields (e.g. UUID via gen_random_uuid()) are available
     * immediately after save. Calls: - flush(): force the INSERT - refresh(): reselect the row with
     * DB defaults/triggers
     */
    private ApplicationListEntry refreshEntity(ApplicationListEntry entity) {
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }

    /**
     * gets the keyable for the create/update entry.
     *
     * @param updateEntry The entry that was created or is being updated
     * @return The list of keyables that constitute an etag
     */
    private List<Keyable> getKeyablesForCreateUpdateEtag(ApplicationListEntry updateEntry) {
        List<AppListEntryOfficial> officialList =
                appListEntryOfficialRepository.getOfficialByEntryUuid(updateEntry.getUuid());
        List<AppListEntryFeeStatus> appListStatus =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(updateEntry.getUuid());
        List<Fee> feesForEntry = appListEntryFeeRepository.getFeeForEntryId(updateEntry.getId());

        // create the update etag based on the following details
        List<Keyable> keyables = new ArrayList<>();
        keyables.add(updateEntry);
        keyables.addAll(officialList);
        keyables.addAll(appListStatus);
        keyables.addAll(feesForEntry);
        return keyables;
    }
}
