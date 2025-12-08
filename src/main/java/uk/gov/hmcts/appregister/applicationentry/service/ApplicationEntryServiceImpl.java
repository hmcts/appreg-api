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
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId_;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.compositeid.AppListEntryFeeCompositeId;
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
import uk.gov.hmcts.appregister.generated.model.Applicant;
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

                                        NameAddress applicantToSave = createApplicant(entryCreateDto);

                                        NameAddress respondentToSave = createRespondent(entryCreateDto);


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

                                        List<AppListEntryFeeStatus> statusList
                                            = createFeeStatus(listEntryEntity, entryCreateDto);

                                        List<AppListEntryOfficial> officialList
                                            = createOfficial(listEntryEntity, entryCreateDto);

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

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                updateApplicationEntryValidator.validate(
                        updateEntry,
                        (dto, success) -> {
                            // lets check the concurrent match before we process the update
                            return matchService.matchOnRequest(
                                    () -> {
                                        return auditService.processAudit(
                                                BeanUtil.copyBean(getEntry(success)),
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
                                                            getEntry(success);

                                                    // update the core list data
                                                    applicationListEntryEntityMapper
                                                            .toApplicationListEntry(
                                                                    updateEntry.getData(),
                                                                    success.getWordingSentence()
                                                                            .substitute(
                                                                                    updateEntry
                                                                                            .getData()
                                                                                            .getWordingFields()),
                                                                    getStandardApplicant(success),
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
                                                            replaceOfficials(updateEntry, success);

                                                    // update the fees for the entry
                                                    updateFees(success);

                                                    // create the fee entry mappings
                                                    EntryGetDetailDto entryGetDetailDto =
                                                            applicationListEntryMapStructMapper
                                                                    .toEntryGetDetailDto(
                                                                            getEntry(success),
                                                                            updatedFeeStatusLst,
                                                                            getFee(success),
                                                                            updatedOfficialList,
                                                                            getStandardApplicant(success));
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
                                                                    getEntry(success)));
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
     * @param success The successful validation result
     * @param listEntryEntity The entry entity
     * @param entryCreateDto The create payload containing the fees
     */
    private void createFees(CreateApplicationEntryValidationSuccess success,
                            ApplicationListEntry listEntryEntity,
                            PayloadForCreate<EntryCreateDto> entryCreateDto) {
        if (success.getFee() != null) {
            // save and audit
            auditService.processAudit(
                AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                req -> {
                    // create the link between the entry and the
                    // fees
                    AppListEntryFeeId appListEntryFeeId =
                        new AppListEntryFeeId();
                    appListEntryFeeId.setAppListEntryId(
                        listEntryEntity.getId());
                    appListEntryFeeId.setFeeId(
                        success.getFee().getId());

                    log.debug(
                        "Created Fee: {} to Entry: {} mapping: {}",
                        appListEntryFeeId.getFeeId(),
                        appListEntryFeeId
                            .getAppListEntryId());

                    return Optional.of(new AuditableResult<>(null,
                                                             appListEntryFeeRepository.save(appListEntryFeeId)));
                });
        }
    }

    /**
     * create all officials for the entry
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the officials
     * @return The application list entry officials that were created
     */
    private List<AppListEntryOfficial> createOfficial(ApplicationListEntry listEntryEntity,
                                                      PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryOfficial> officialList = new ArrayList<>();
        if (entryCreateDto.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official :
                entryCreateDto.getData().getOfficials()) {

                // save and audit
                auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY,
                    req -> {
                        AppListEntryOfficial newOfficialEntity = appListEntryOfficialRepository.save(
                            applicationListEntryEntityMapper
                                .toOfficial(
                                    official,
                                    listEntryEntity
                                ));

                        log.debug(
                            "Original created and mapped to application entry with id: {}",
                            newOfficialEntity.getId());
                        officialList.add(newOfficialEntity);

                        return Optional.of(new AuditableResult<>(null,
                                                                 newOfficialEntity));
                    });
            }
        }
        return officialList;
    }

    /**
     * create all fee statuses and map them to the entry
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the officials
     * @return The application fees that were created
     */
    private List<AppListEntryFeeStatus>  createFeeStatus(ApplicationListEntry listEntryEntity,
                                                         PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryFeeStatus> statusList = new ArrayList<>();

        if (entryCreateDto.getData().getFeeStatuses() != null) {
            // create the fee statuses and map to entry
            for (FeeStatus feeStatus :
                entryCreateDto.getData().getFeeStatuses()) {

                auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                    req -> {
                        AppListEntryFeeStatus createdAppListStatus =
                            appListEntryFeeStatusRepository.save(
                                applicationListEntryEntityMapper
                                    .toFeeStatus(
                                        feeStatus,
                                        listEntryEntity
                                    ));
                        statusList.add(createdAppListStatus);
                        log.debug(
                            "Fee status created and mapped to application "
                                + "entry with id: {}",
                            createdAppListStatus.getId()
                        );
                        return Optional.of(new AuditableResult<>(
                            null,
                            createdAppListStatus
                        ));
                    });
            }
        }

        return statusList;
    }

    /**
     * creates the applicant
     * @param entryCreateDto The applicant data to create
     * @return The created applicant
     */
    private NameAddress createApplicant(PayloadForCreate<EntryCreateDto> entryCreateDto) {
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

            applicantToSave = auditService.processAudit(
                AppListEntryAuditOperation.CREATE_APPLICANT,
                req -> {
                    NameAddress applicantToAdded =
                        applicantMapper.toApplicant(
                            entryCreateDto
                                .getData()
                                .getApplicant());
                    nameAddressRepository.save(applicantToAdded);
                    log.debug(
                        "Created applicant with id: {}",
                        applicantToAdded.getId()
                    );

                    return Optional.of(new AuditableResult<>(
                        applicantToAdded,
                        applicantToAdded
                    ));
                });
        }
        return applicantToSave;
    }

    /**
     * creates the respondent
     * @param entryCreateDto The applicant data to create
     * @return The created respondent
     */
    private NameAddress createRespondent(PayloadForCreate<EntryCreateDto> entryCreateDto) {
        // save the respondent
        NameAddress respondentToSave = null;
        if (entryCreateDto.getData().getRespondent() != null) {
            respondentToSave = auditService.processAudit(
                AppListEntryAuditOperation.CREATE_RESPONDENT,
                req -> {
                    NameAddress respondentToAdded =
                        nameAddressRepository.save(
                            applicantMapper.toRespondent(
                                entryCreateDto
                                    .getData()
                                    .getRespondent()));
                    log.debug(
                        "Created respondent with id: {}",
                        respondentToAdded.getId()
                    );

                    return Optional.of(new AuditableResult<>(
                        respondentToAdded,
                        respondentToAdded
                    ));
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
        // capture the respondent before the change
        NameAddress existingRespondent = null;
        if (success.getApplicationEntryId().getRnameaddress() != null) {
            existingRespondent = BeanUtil.copyBean(getEntry(success).getRnameaddress());
        }

        // if we are not expecting a respondent set to null
        if (updateEntry.getData().getRespondent() != null) {
            NameAddress nameAddress = auditService.processAudit(
                AppListEntryAuditOperation.CREATE_RESPONDENT,
                req -> {
                    NameAddress na = nameAddressRepository.save(
                        applicantMapper.toRespondent(updateEntry.getData().getRespondent()));
                    return Optional.of(new AuditableResult<>(na, na));
                });
            success.getApplicationEntryId().setRnameaddress(nameAddress);
        } else {
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
                        getEntry(success).getId()
                    );

                    return Optional.empty();
                }
            );
        }

        nameAddressRepository.flush();
    }

    /**
     * gets the entry fresh from the success entry that was established. The entry may have been updated
     * so to avoid an Optimsitic locking exception use this.
     * @param success The successful validation result
     * @return The entry
     */
    private ApplicationListEntry getEntry(UpdateApplicationEntryValidationSuccess success) {
        return applicationListEntryRepository.findById(success.getApplicationEntryId().getId()).get();
    }


    /**
     * gets the fee from the success entry that was established.
     * @param success The successful validation result
     * @return The fee
     */
    private Fee getFee(UpdateApplicationEntryValidationSuccess success) {
        if (success.getFee()!=null) {
            return feeRepository.findById(success.getFee().getId()).get();
        }

        return null;
    }

    /**
     * gets the standard applicant from the success entry that was established.
     *
     * @param success The successful validation result
     * @return The standard applicant
     */
    private StandardApplicant getStandardApplicant(UpdateApplicationEntryValidationSuccess success) {
        if (success.getSa() != null) {
            return standardApplicantRepository.findById(success.getSa().getId()).get();
        }

        return success.getSa();
    }


    /**
     * Updates the applicant. Deletes the old applicant.
     * @param updateEntry the update payload
     * @param success The success validation response
     */
    private void updateApplicant(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        // save the applicant
        NameAddress applicantToSave = null;

        // capture the applicant before the change
        NameAddress existingApplicant = null;
        if (success.getApplicationEntryId().getAnamedaddress() != null) {
            existingApplicant = BeanUtil.copyBean(getEntry(success).getAnamedaddress());
        }

        if (updateEntry.getData().getApplicant() != null
                        && (updateEntry.getData().getApplicant().getOrganisation() != null
                || updateEntry.getData().getApplicant().getPerson() != null)) {

            // set the standard applicant
            success.getApplicationEntryId().setStandardApplicant(null);

            NameAddress nameAddress = auditService.processAudit(
                AppListEntryAuditOperation.CREATE_APPLICANT,
                req -> {
                    // now add the new applicant
                    NameAddress applicant = applicantMapper.toApplicant(updateEntry.getData().getApplicant());
                    NameAddress na = nameAddressRepository.save(applicant);
                    return Optional.of(new AuditableResult<>(na, na));
                });

            log.debug("Update applicant with id: {}", nameAddress.getId());
            success.getApplicationEntryId().setAnamedaddress(nameAddress);
        } else if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
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
                    nameAddressRepository.flush();
                    log.debug("Deleted old applicant with id: {}", req.getOldValue().getId());
                    return Optional.empty();
                });
        }
        nameAddressRepository.flush();
    }

    /**
     * appends to the existing fee status.
     * @param updateEntry The update payload
     * @param success The successful validation result
     */
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
                auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                    req -> {
                        AppListEntryFeeStatus createdAppListStatus =
                            appListEntryFeeStatusRepository.save(
                                applicationListEntryEntityMapper.toFeeStatus(
                                    feeStatus, getEntry(success)));

                        statusList.add(createdAppListStatus);
                        log.debug(
                            "Fee status created and " + "mapped to application " + "entry with id: {}",
                            createdAppListStatus.getId());
                        return Optional.of(new AuditableResult<>(null, createdAppListStatus));
                    });
            }

            appListEntryFeeStatusRepository.flush();
        }

        return statusList;
    }

    /**
     * updates the fees for the entry.
     * @param success The successful validation result
     */
    private void updateFees(UpdateApplicationEntryValidationSuccess success) {
        // deletes all the fees
        List<AppListEntryFeeId> appListEntryFeeIdList
            = appListEntryFeeRepository.getEntryFeesForEntry(getEntry(success).getId());
        for (AppListEntryFeeId feeId : appListEntryFeeIdList) {
            auditService.processAudit(
                feeId,
                AppListEntryAuditOperation.DELETE_FEE_ENTRY,
                req -> {

                    return Optional.empty();
                });
        }
        appListEntryFeeRepository.deleteForEntryId(getEntry(success).getId());
        appListEntryFeeRepository.flush();

        // add a new fee mapping if we have a fee
        if (success.getFee() != null) {
            auditService.processAudit(
                AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                req -> {
                    // create the link between the entry and the
                    // fees
                    AppListEntryFeeId appListEntryFeeId =
                        new AppListEntryFeeId();
                    appListEntryFeeId.setAppListEntryId(
                        getEntry(success).getId());
                    appListEntryFeeId.setFeeId(
                        success.getFee().getId());

                    appListEntryFeeId =
                        appListEntryFeeRepository.save(
                            appListEntryFeeId);
                    log.debug(
                        "Created Fee: {} to Entry: {} mapping: {}",
                        appListEntryFeeId.getFeeId(),
                        appListEntryFeeId
                            .getAppListEntryId()
                    );

                    return Optional.of(new AuditableResult<>(null, appListEntryFeeId));
                }
            );
        }
        appListEntryFeeRepository.flush();

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
     * replaces the officials for the entry.
     *
     * @param success The success validation
     * @return The update officials
     */
    private List<AppListEntryOfficial> replaceOfficials(
            PayloadForUpdateEntry payload, UpdateApplicationEntryValidationSuccess success) {

        List<AppListEntryOfficial> officials = appListEntryOfficialRepository.getOfficialByEntryUuid(success.getApplicationEntryId().getUuid());

        // delete existing officials and audit each
        for (AppListEntryOfficial off : officials) {
            auditService.processAudit(
                off,
                AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY,
                req -> {
                    // delete the officials that already exist
                    appListEntryOfficialRepository.deleteAllForEntryId(
                        getEntry(success).getId());
                    return Optional.empty();
                }
            );
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
                                    official, getEntry(success)));
                        officialList.add(createdOriginal);
                        log.debug(
                            "Original created and mapped to application " + "entry with id: {}",
                            createdOriginal.getId()
                        );
                        return Optional.of(new AuditableResult<>(null,
                                                           createdOriginal));
                    });
            }
        }
        appListEntryOfficialRepository.flush();

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
