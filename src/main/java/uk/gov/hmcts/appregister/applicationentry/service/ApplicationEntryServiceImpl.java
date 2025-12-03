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
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
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
import uk.gov.hmcts.appregister.common.util.BeanUtil;
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

    private final UpdateApplicationEntryValidator updateApplicationEntryValidator;

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
        resultPage.map(
                entry -> {
                    return newPage.addContentItem(
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
        MatchResponse<EntryGetDetailDto> getDetailDto =
                createApplicationEntryValidator.validate(
                        entryCreateDto,
                        (dto, success) -> {
                            return auditService.processAudit(
                                    AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST,
                                    req -> {

                                        // save the applicant
                                        NameAddress applicantToSave = null;
                                        if (entryCreateDto.getData().getApplicant() != null
                                                        && entryCreateDto
                                                                        .getData()
                                                                        .getApplicant()
                                                                        .getOrganisation()
                                                                != null
                                                || entryCreateDto
                                                                .getData()
                                                                .getApplicant()
                                                                .getPerson()
                                                        != null) {
                                            applicantToSave =
                                                    applicantMapper.toApplicant(
                                                            entryCreateDto
                                                                    .getData()
                                                                    .getApplicant());
                                            nameAddressRepository.save(applicantToSave);
                                            log.debug(
                                                    "Created applicant with id: {}",
                                                    applicantToSave.getId());
                                        }

                                        // save the respondent
                                        NameAddress respondentToSave = null;
                                        if (entryCreateDto.getData().getRespondent() != null) {
                                            respondentToSave =
                                                    nameAddressRepository.save(
                                                            applicantMapper.toRespondent(
                                                                    entryCreateDto
                                                                            .getData()
                                                                            .getRespondent()));
                                            log.debug(
                                                    "Created respondent with id: {}",
                                                    respondentToSave.getId());
                                        }

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

                                        List<AppListEntryFeeStatus> statusList = new ArrayList<>();

                                        if (entryCreateDto.getData().getFeeStatuses() != null) {
                                            // create the fee statuses and map to entry
                                            for (FeeStatus feeStatus :
                                                    entryCreateDto.getData().getFeeStatuses()) {
                                                AppListEntryFeeStatus createdAppListStatus =
                                                        appListEntryFeeStatusRepository.save(
                                                                applicationListEntryEntityMapper
                                                                        .toFeeStatus(
                                                                                feeStatus,
                                                                                listEntryEntity));
                                                statusList.add(createdAppListStatus);
                                                log.debug(
                                                        "Fee status created and mapped to application "
                                                                + "entry with id: {}",
                                                        createdAppListStatus.getId());
                                            }
                                        }

                                        List<AppListEntryOfficial> officialList = new ArrayList<>();

                                        if (entryCreateDto.getData().getOfficials() != null) {
                                            // create the official for the entry
                                            for (Official official :
                                                    entryCreateDto.getData().getOfficials()) {
                                                AppListEntryOfficial createdOriginal =
                                                        appListEntryOfficialRepository.save(
                                                                applicationListEntryEntityMapper
                                                                        .toOfficial(
                                                                                official,
                                                                                listEntryEntity));
                                                officialList.add(createdOriginal);
                                                log.debug(
                                                        "Original created and mapped to application entry with id: {}",
                                                        createdOriginal.getId());
                                            }
                                        }

                                        if (success.getFee() != null) {
                                            // create the link between the entry and the fees
                                            AppListEntryFeeId appListEntryFeeId =
                                                    new AppListEntryFeeId();
                                            appListEntryFeeId.setAppListEntryId(listEntryEntity);
                                            appListEntryFeeId.setFeeId(success.getFee());

                                            appListEntryFeeId =
                                                    appListEntryFeeRepository.save(
                                                            appListEntryFeeId);
                                            log.debug(
                                                    "Created Fee: {} to Entry: {} mapping: {}",
                                                    appListEntryFeeId.getFeeId(),
                                                    appListEntryFeeId
                                                            .getAppListEntryId()
                                                            .getEntryFeeIds());
                                        }

                                        listEntryEntity = refreshEntity(listEntryEntity);

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
                                    },
                                    auditLifecycleListeners.toArray(
                                            new AuditOperationLifecycleListener[0]));
                        });

        log.debug("Finish: Create Application Entry: {}", entryCreateDto);

        return getDetailDto;
    }

    /**
     * gets the keyable for the create/update entry.
     *
     * @param updateEntry The entry that was created or is being updated
     * @return The list of keyables
     */
    private List<Keyable> getKeyablesForCreateUpdateEtag(ApplicationListEntry updateEntry) {
        List<AppListEntryOfficial> officialList =
                appListEntryOfficialRepository.getOfficialByEntryUuid(updateEntry.getUuid());
        List<AppListEntryFeeStatus> appListStatus =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(updateEntry.getUuid());

        // create the update etag based on the following details
        List<Keyable> keyables = new ArrayList<>();
        keyables.add(updateEntry);
        keyables.addAll(officialList);
        keyables.addAll(appListStatus);
        return keyables;
    }

    @Override
    @Transactional
    public MatchResponse<EntryGetDetailDto> updateEntry(PayloadForUpdateEntry updateEntry) {
        log.debug("Started: Update Application Entry: {}", updateEntry);

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                updateApplicationEntryValidator.validate(
                        updateEntry,
                        (dto, success) -> {
                            // lets check the concurrent match before we process the update
                            return matchService.matchOnRequest(
                                    () -> {
                                        return auditService.processAudit(
                                                BeanUtil.copyBean(success.getApplicationEntryId()),
                                                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST,
                                                req -> {

                                                    // save the applicant
                                                    updateApplicant(updateEntry, success);

                                                    // save the respondent
                                                    updateRespondent(updateEntry, success);

                                                    updateStandardApplicant(success);

                                                    // save the list
                                                    ApplicationListEntry listEntryEntity =
                                                            success.getApplicationEntryId();

                                                    // update the core list data
                                                    applicationListEntryEntityMapper
                                                            .toApplicationListEntry(
                                                                    updateEntry.getData(),
                                                                    success.getWordingSentence()
                                                                            .substitute(
                                                                                    updateEntry
                                                                                            .getData()
                                                                                            .getWordingFields()),
                                                                    success.getSa(),
                                                                    success.getApplicationCode(),
                                                                    success.getApplicationList(),
                                                                    success
                                                                            .getApplicationEntryId());

                                                    // save the core list data
                                                    listEntryEntity =
                                                            refreshEntity(
                                                                    applicationListEntryRepository
                                                                            .save(listEntryEntity));
                                                    log.debug(
                                                            "Created application entry with id: {}",
                                                            listEntryEntity.getId());

                                                    // add the new fee statuses
                                                    List<AppListEntryFeeStatus>
                                                            updatedFeeStatusLst =
                                                                    updateFeeStatus(
                                                                            updateEntry, success);

                                                    // update the officials
                                                    List<AppListEntryOfficial> updatedOfficialList =
                                                            replaceOfficials(updateEntry, success);

                                                    if (success.getFee() != null) {
                                                        // create the link between the entry and the
                                                        // fees
                                                        AppListEntryFeeId appListEntryFeeId =
                                                                new AppListEntryFeeId();
                                                        appListEntryFeeId.setAppListEntryId(
                                                                listEntryEntity);
                                                        appListEntryFeeId.setFeeId(
                                                                success.getFee());

                                                        appListEntryFeeId =
                                                                appListEntryFeeRepository.save(
                                                                        appListEntryFeeId);
                                                        log.debug(
                                                                "Created Fee: {} to Entry: {} mapping: {}",
                                                                appListEntryFeeId.getFeeId(),
                                                                appListEntryFeeId
                                                                        .getAppListEntryId()
                                                                        .getEntryFeeIds());
                                                    }

                                                    // create the fee entry mappings
                                                    EntryGetDetailDto entryGetDetailDto =
                                                            applicationListEntryMapStructMapper
                                                                    .toEntryGetDetailDto(
                                                                            listEntryEntity,
                                                                            updatedFeeStatusLst,
                                                                            success.getFee(),
                                                                            updatedOfficialList,
                                                                            success.getSa());
                                                    entryGetDetailDto.setHasOffsiteFee(
                                                            updateEntry
                                                                    .getData()
                                                                    .getHasOffsiteFee());

                                                    return Optional.of(
                                                            new AuditableResult<>(
                                                                    MatchResponse.of(
                                                                            entryGetDetailDto,
                                                                            getKeyablesForCreateUpdateEtag(
                                                                                    listEntryEntity)),
                                                                    listEntryEntity));
                                                },
                                                auditLifecycleListeners.toArray(
                                                        new AuditOperationLifecycleListener[0]));
                                    },

                                    // return the latest entities for the entry read on the update
                                    getKeyablesForCreateUpdateEtag(
                                            success.getApplicationEntryId()));
                        });

        log.debug("Finish: Update Application Entry: {}", updateEntry);

        return getDetailDto;
    }

    /**
     * Updates the applicant. Deletes the old respondent.
     *
     * @param updateEntry the update payload
     */
    private void updateRespondent(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        // save the respondent
        NameAddress respondentToSave = null;
        if (updateEntry.getData().getRespondent() != null) {

            // delete the respondent that already exists
            if (success.getApplicationEntryId().getRnameaddress() != null) {
                nameAddressRepository.delete(success.getApplicationEntryId().getRnameaddress());
                log.debug(
                        "Deleted old respondent with id: {}",
                        success.getApplicationEntryId().getId());
            }

            respondentToSave =
                    nameAddressRepository.save(
                            applicantMapper.toRespondent(updateEntry.getData().getRespondent()));
            success.getApplicationEntryId().setRnameaddress(respondentToSave);
            log.debug("Update respondent with id: {}", respondentToSave.getId());
        }
    }

    /** Updates the applicant. Deletes the old applicant. */
    private void updateApplicant(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        // save the applicant
        NameAddress applicantToSave = null;
        if (updateEntry.getData().getApplicant() != null
                        && updateEntry.getData().getApplicant().getOrganisation() != null
                || updateEntry.getData().getApplicant().getPerson() != null) {

            NameAddress existingApplicant = success.getApplicationEntryId().getAnamedaddress();

            // delete the applicant that already exists
            if (existingApplicant != null) {
                nameAddressRepository.delete(existingApplicant);
                log.debug("Deleted old applicant with id: {}", existingApplicant.getId());
            }

            // set the standard applicant
            success.getApplicationEntryId().setStandardApplicant(null);

            // now add the new applicant
            applicantToSave = applicantMapper.toApplicant(updateEntry.getData().getApplicant());
            nameAddressRepository.save(applicantToSave);
            log.debug("Update applicant with id: {}", applicantToSave.getId());
            success.getApplicationEntryId().setAnamedaddress(applicantToSave);
        } else if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
            success.getApplicationEntryId().setAnamedaddress(null);
        }
    }

    private List<AppListEntryFeeStatus> updateFeeStatus(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {

        // gets all of the existing status
        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(updateEntry.getEntryId());

        // add the new fee statuses
        List<AppListEntryFeeStatus> statusList = new ArrayList<>(feeStatuses);

        if (updateEntry.getData().getFeeStatuses() != null) {
            // create the fee statuses and map to entry
            for (FeeStatus feeStatus : updateEntry.getData().getFeeStatuses()) {
                AppListEntryFeeStatus createdAppListStatus =
                        appListEntryFeeStatusRepository.save(
                                applicationListEntryEntityMapper.toFeeStatus(
                                        feeStatus, success.getApplicationEntryId()));
                statusList.add(createdAppListStatus);
                log.debug(
                        "Fee status created and " + "mapped to application " + "entry with id: {}",
                        createdAppListStatus.getId());
            }
        }

        return statusList;
    }

    /**
     * Updates the respondent. Deletes the old respondent.
     *
     * @param success The successful validation result
     */
    private void updateStandardApplicant(UpdateApplicationEntryValidationSuccess success) {
        if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
            success.getApplicationEntryId().setAnamedaddress(null);
        }
    }

    /**
     * updates the officials for the entry.
     *
     * @param success The success validation
     * @return The update officials
     */
    private List<AppListEntryOfficial> replaceOfficials(
            PayloadForUpdateEntry payload, UpdateApplicationEntryValidationSuccess success) {
        // delete the officials that already exist
        appListEntryOfficialRepository.deleteAllForEntryUuid(
                success.getApplicationEntryId().getUuid());

        // add officials
        List<AppListEntryOfficial> officialList = new ArrayList<>();

        if (payload.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official : payload.getData().getOfficials()) {
                AppListEntryOfficial createdOriginal =
                        appListEntryOfficialRepository.save(
                                applicationListEntryEntityMapper.toOfficial(
                                        official, success.getApplicationEntryId()));
                officialList.add(createdOriginal);
                log.debug(
                        "Original created and mapped to application " + "entry with id: {}",
                        createdOriginal.getId());
            }
        }

        return officialList;
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
}
