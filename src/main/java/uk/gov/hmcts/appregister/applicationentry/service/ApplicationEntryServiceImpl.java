package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
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
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
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

    private final FeeRepository feeRepository;

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
    private final StandardApplicantRepository standardApplicantRepository;

    private final ApplicationListEntryMapper applicationListEntryMapStructMapper;
    private final ApplicantMapper applicantMapper;

    private final ApplicationListEntryEntityMapper applicationListEntryEntityMapper;

    // Infrastructure
    private final EntityManager entityManager;

    private final GetApplicationEntryValidator getEntryValidator;

    private final GetApplicationListEntriesValidator getApplicationListEntriesValidator;

    private final Clock clock;

    @Override
    public EntryPage search(EntryGetFilterDto filterDto, PagingWrapper pageable) {
        Status status = applicationListEntryMapStructMapper.toStatus(filterDto.getStatus());

        log.debug(
                "Started: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                applicationListEntryRepository.searchForGetSummary(
                        null,
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
                        pageable.getPageable());

        // breaks name into individual and/or organisation parts
        EntryPage newPage = new EntryPage();
        pageMapper.toPage(resultPage, newPage, pageable.getSortStrings());

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
        log.debug("Creating application entry inside list {}", entryCreateDto.getId());

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                createApplicationEntryValidator.validate(
                        entryCreateDto,
                        (dto, success) -> {
                            return auditService.processAudit(
                                    AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST,
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
                                                                                        .getWordingFields())
                                                                        .getSubstitutedString(),
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

    @Override
    @Transactional
    public MatchResponse<EntryGetDetailDto> updateEntry(PayloadForUpdateEntry updateEntry) {
        log.debug("Started: Update Application Entry: {}", updateEntry);
        log.debug(
                "Updating application entry with id: {} in list {}",
                updateEntry.getEntryId(),
                updateEntry.getId());

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

                                                    // update entry with a standard applicant
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
                                                                                            .getWordingFields())
                                                                            .getSubstitutedString(),
                                                                    success.getSa(),
                                                                    success.getApplicationCode(),
                                                                    success.getApplicationList(),
                                                                    listEntryEntity);

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
                                                            updateOfficials(updateEntry, success);

                                                    // update the fees for the entry
                                                    updateFees(success);

                                                    // create the fee entry mappings
                                                    EntryGetDetailDto entryGetDetailDto =
                                                            applicationListEntryMapStructMapper
                                                                    .toEntryGetDetailDto(
                                                                            success
                                                                                    .getApplicationEntryId(),
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
                                                                    success
                                                                            .getApplicationEntryId()));
                                                });
                                    },

                                    // return the latest entities for the entry read on the update
                                    getKeyablesForCreateUpdateEtag(
                                            success.getApplicationEntryId()));
                        });

        log.debug("Finish: Update Application Entry: {}", updateEntry);

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
     * Updates the applicant. Deletes the old respondent.
     *
     * @param updateEntry the update payload
     * @param success The success validation response
     */
    private void updateRespondent(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating respondent");

        // capture the respondent before the change
        NameAddress existingRespondent = null;
        if (success.getApplicationEntryId().getRnameaddress() != null) {
            existingRespondent =
                    BeanUtil.copyBean(success.getApplicationEntryId().getRnameaddress());
        }

        // if we are not expecting a respondent set to null
        if (updateEntry.getData().getRespondent() != null
                && (updateEntry.getData().getRespondent().getOrganisation() != null
                        || updateEntry.getData().getRespondent().getPerson() != null)) {
            NameAddress nameAddress =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_RESPONDENT,
                            req -> {
                                NameAddress na =
                                        nameAddressRepository.save(
                                                applicantMapper.toRespondent(
                                                        updateEntry.getData().getRespondent()));
                                return Optional.of(new AuditableResult<>(na, na));
                            });
            log.debug("Assigning new respondent {}", nameAddress.getId());
            success.getApplicationEntryId().setRnameaddress(nameAddress);
        } else {
            log.debug("No respondent present. Setting respondent to null");
            success.getApplicationEntryId().setRnameaddress(null);
        }

        applicationListEntryRepository.save(success.getApplicationEntryId());
        applicationListEntryRepository.flush();

        if (existingRespondent != null) {
            auditService.processAudit(
                    existingRespondent,
                    AppListEntryAuditOperation.DELETE_RESPONDENT,
                    req -> {
                        // delete the respondent that already exists
                        nameAddressRepository.deleteForId(req.getOldValue().getId());
                        log.debug(
                                "Deleted old respondent with id: {}",
                                success.getApplicationEntryId().getId());

                        return Optional.empty();
                    });
        }
    }

    /**
     * Updates the applicant. Deletes the old applicant.
     *
     * @param updateEntry the update payload
     * @param success The success validation response
     */
    private void updateApplicant(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating applicant");

        // capture the applicant before the change
        NameAddress existingApplicant = null;
        if (success.getApplicationEntryId().getAnamedaddress() != null) {
            existingApplicant =
                    BeanUtil.copyBean(success.getApplicationEntryId().getAnamedaddress());
        }

        if (updateEntry.getData().getApplicant() != null
                && (updateEntry.getData().getApplicant().getOrganisation() != null
                        || updateEntry.getData().getApplicant().getPerson() != null)) {

            // set the standard applicant
            success.getApplicationEntryId().setStandardApplicant(null);

            NameAddress nameAddress =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_APPLICANT,
                            req -> {
                                // now add the new applicant
                                NameAddress applicant =
                                        applicantMapper.toApplicant(
                                                updateEntry.getData().getApplicant());
                                NameAddress na = nameAddressRepository.save(applicant);
                                log.debug("Assigning new applicant {}", na.getId());
                                return Optional.of(new AuditableResult<>(na, na));
                            });

            log.debug("Update applicant with id: {}", nameAddress.getId());
            success.getApplicationEntryId().setAnamedaddress(nameAddress);
        } else if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
            success.getApplicationEntryId().setAnamedaddress(null);

            log.debug("No applicant present. Using standard applicant {}", success.getSa().getId());
        } else {
            log.debug("No applicant present. Setting applicant to null");
            success.getApplicationEntryId().setAnamedaddress(null);
        }

        applicationListEntryRepository.save(success.getApplicationEntryId());
        applicationListEntryRepository.flush();

        // delete the applicant that already exists
        if (existingApplicant != null) {
            auditService.processAudit(
                    existingApplicant,
                    AppListEntryAuditOperation.DELETE_APPLICANT,
                    req -> {
                        nameAddressRepository.deleteForId(req.getOldValue().getId());
                        log.debug("Deleted old applicant with id: {}", req.getOldValue().getId());
                        return Optional.empty();
                    });
        }
    }

    /**
     * appends to the existing fee status.
     *
     * @param updateEntry The update payload
     * @param success The successful validation result
     */
    private List<AppListEntryFeeStatus> updateFeeStatus(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating fee status");

        // gets all of the existing status
        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(updateEntry.getEntryId());

        // add the new fee statuses
        List<AppListEntryFeeStatus> statusList = new ArrayList<>(feeStatuses);

        if (updateEntry.getData().getFeeStatuses() != null) {
            // create the fee statuses and map to entry
            for (FeeStatus feeStatus : updateEntry.getData().getFeeStatuses()) {
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                        req -> {
                            AppListEntryFeeStatus createdAppListStatus =
                                    appListEntryFeeStatusRepository.save(
                                            applicationListEntryEntityMapper.toFeeStatus(
                                                    feeStatus, success.getApplicationEntryId()));

                            statusList.add(createdAppListStatus);
                            log.debug(
                                    "Fee status created and "
                                            + "mapped to application "
                                            + "entry with id: {}",
                                    createdAppListStatus.getId());
                            return Optional.of(new AuditableResult<>(null, createdAppListStatus));
                        });
            }
        }

        return statusList;
    }

    /**
     * updates the fees for the entry.
     *
     * @param success The successful validation result
     */
    private void updateFees(UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating fees");
        // deletes all the fees
        List<AppListEntryFeeId> appListEntryFeeIdList =
                appListEntryFeeRepository.getEntryFeesForEntry(
                        success.getApplicationEntryId().getId());
        for (AppListEntryFeeId feeId : appListEntryFeeIdList) {
            // if the fee is not the one we are updating delete it
            if (success.getFee() == null
                    || feeId.getFeeId().longValue() != success.getFee().getId()) {
                auditService.processAudit(
                        feeId,
                        AppListEntryAuditOperation.DELETE_FEE_ENTRY,
                        req -> {
                            appListEntryFeeRepository.delete(feeId);
                            return Optional.empty();
                        });
            }
        }

        appListEntryFeeRepository.flush();

        // if we have a fee, remove all other fees associated with the entry
        if (success.getFee() != null) {
            log.debug("A fee update is present for fee {}", success.getFee().getId());

            Optional<AppListEntryFeeId> appListEntryFeeId =
                    appListEntryFeeRepository.getEntryFeesForFee(
                            success.getApplicationEntryId().getId(), success.getFee().getId());

            // if we have no fees associated then create a new one
            if (!appListEntryFeeId.isPresent()) {
                log.debug(
                        "Adding new fee {} to entry %s {}",
                        success.getFee().getId(), success.getApplicationEntryId().getId());
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                        req -> {
                            // create the link between the entry and the
                            // fees
                            AppListEntryFeeId newAppListEntryFeeId = new AppListEntryFeeId();
                            newAppListEntryFeeId.setAppListEntryId(
                                    success.getApplicationEntryId().getId());
                            newAppListEntryFeeId.setFeeId(success.getFee().getId());

                            newAppListEntryFeeId =
                                    appListEntryFeeRepository.save(newAppListEntryFeeId);
                            log.debug(
                                    "Created Fee: {} to Entry: {} mapping: {}",
                                    newAppListEntryFeeId.getFeeId(),
                                    newAppListEntryFeeId.getAppListEntryId());

                            return Optional.of(new AuditableResult<>(null, newAppListEntryFeeId));
                        });
            }
        }
    }

    /**
     * Updates the standard applicant. Deletes the old applicant.
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
    private List<AppListEntryOfficial> updateOfficials(
            PayloadForUpdateEntry payload, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating officials");

        List<AppListEntryOfficial> officials =
                appListEntryOfficialRepository.getOfficialByEntryUuid(
                        success.getApplicationEntryId().getUuid());

        // delete existing officials and audit each
        for (AppListEntryOfficial off : officials) {
            auditService.processAudit(
                    off,
                    AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY,
                    req -> {
                        log.debug("Deleting officials");

                        // delete the officials that already exist
                        appListEntryOfficialRepository.deleteAllForEntryId(
                                success.getApplicationEntryId().getId());
                        return Optional.empty();
                    });
        }

        // add officials
        List<AppListEntryOfficial> officialList = new ArrayList<>();
        if (payload.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official : payload.getData().getOfficials()) {
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY,
                        req -> {
                            AppListEntryOfficial createdOriginal =
                                    appListEntryOfficialRepository.save(
                                            applicationListEntryEntityMapper.toOfficial(
                                                    official, success.getApplicationEntryId()));
                            officialList.add(createdOriginal);
                            log.debug(
                                    "Original created and mapped to application "
                                            + "entry with id: {}",
                                    createdOriginal.getId());
                            return Optional.of(new AuditableResult<>(null, createdOriginal));
                        });
            }
        }

        return officialList;
    }

    @Override
    public MatchResponse<EntryGetDetailDto> getApplicationListEntryDetail(
            PayloadGetEntryInList entry) {
        log.debug(
                "Started: Getting application list entry detail: {} for list: {}",
                entry.getEntryId(),
                entry.getListId());

        return getEntryValidator.validate(
                entry,
                (req, success) -> {
                    getKeyablesForCreateUpdateEtag(success.getApplicationListEntry());
                    EntryGetDetailDto dto =
                            applicationListEntryMapStructMapper.toEntryGetDetailDto(
                                    success.getApplicationListEntry(),
                                    hasOffsite(success.getApplicationListEntry()));
                    log.debug(
                            "Finished: Getting application list entry detail: {} for list: {}",
                            entry.getEntryId(),
                            entry.getListId());

                    return MatchResponse.of(
                            dto, getKeyablesForCreateUpdateEtag(success.getApplicationListEntry()));
                });
    }

    @Override
    public EntryPage getApplicationListEntries(
            PayloadGetEntryInList payloadForGet,
            PagingWrapper pageable,
            EntryGetFilterDto filterDto) {
        log.debug(
                "Started: Getting application list entries for list: {}",
                payloadForGet.getListId());

        Status status = applicationListEntryMapStructMapper.toStatus(filterDto.getStatus());

        return getApplicationListEntriesValidator.validate(
                payloadForGet,
                (req, success) -> {
                    // get the entries for the list
                    Page<ApplicationListEntryGetSummaryProjection> entries =
                            applicationListEntryRepository.searchForGetSummary(
                                    payloadForGet.getListId(),
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
                                    pageable.getPageable());

                    EntryPage entryPage = new EntryPage();
                    pageMapper.toPage(entries, entryPage, pageable.getSortStrings());

                    entries.forEach(
                            entry -> {
                                entryPage.addContentItem(
                                        applicationListEntryMapStructMapper.toEntrySummary(entry));
                            });

                    if (entryPage.getContent() == null) {
                        entryPage.setContent(List.of());
                    }

                    log.debug(
                            "Finished: Getting application list entries for list: {}",
                            payloadForGet.getListId());

                    return entryPage;
                });
    }

    /**
     * has an offsite fee for the entry.
     *
     * @param entry The entry
     * @return Whether we have an offsite fee
     */
    private boolean hasOffsite(ApplicationListEntry entry) {
        if (!entry.getEntryFeeIds().isEmpty()) {
            List<Long> feeIds =
                    entry.getEntryFeeIds().stream().map(AppListEntryFeeId::getFeeId).toList();

            if (!feeIds.isEmpty()) {
                List<Fee> fees = feeRepository.findByIdsBetweenDate(feeIds, LocalDate.now(clock));

                // take the first fee offsite if it exists
                return !fees.isEmpty() && fees.getFirst().isOffsite();
            }
        }
        return false;
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
