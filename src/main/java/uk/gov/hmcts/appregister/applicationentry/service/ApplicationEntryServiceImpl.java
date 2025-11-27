package uk.gov.hmcts.appregister.applicationentry.service;

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
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
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
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
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

    private final ApplicationListEntryMapStructMapper mapper;

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

    private final ApplicationListEntryMapStructMapper applicationListEntryMapStructMapper;
    private final ApplicationListEntryEntityMapper applicationListEntryEntityMapper;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Override
    public EntryPage search(EntryGetFilterDto filterDto, Pageable pageable) {
        Status status = ApplicationListEntryMapStructMapper.toStatus(filterDto.getStatus());

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
                    return newPage.addContentItem(mapper.toEntrySummary(entry));
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
                                        if (entryCreateDto.getData().getApplicant() != null &&
                                        entryCreateDto.getData().getApplicant().getOrganisation() != null ||
                                        entryCreateDto.getData().getApplicant().getPerson() != null) {
                                            applicantToSave =
                                                    applicationListEntryEntityMapper.toApplicant(
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
                                                            applicationListEntryEntityMapper
                                                                    .toRespondent(
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
                                                applicationListEntryRepository.save(
                                                        listEntryEntity);
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
                                                                listEntryEntity
                                                            ));
                                                statusList.add(createdAppListStatus);
                                                log.debug(
                                                    "Fee status created and mapped to application entry with id: {}",
                                                    createdAppListStatus.getId()
                                                );
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
                                                                    listEntryEntity
                                                                ));
                                                    officialList.add(createdOriginal);
                                                    log.debug(
                                                        "Original created and mapped to application entry with id: {}",
                                                        createdOriginal.getId()
                                                    );
                                                }
                                            }

                                        EntryGetDetailDto entryGetDetailDto =
                                                applicationListEntryMapStructMapper
                                                        .toEntryGetDetailDto(
                                                                listEntryEntity,
                                                                statusList,
                                                                success.getFee(),
                                                                officialList,
                                                                success.getSa());


                                        if (success.getFee() != null) {
                                            // create the link between the entry and the fees
                                            AppListEntryFeeId appListEntryFeeId =
                                                new AppListEntryFeeId();
                                            appListEntryFeeId.setAppListEntryId(listEntryEntity);
                                            appListEntryFeeId.setFeeId(success.getFee());

                                            appListEntryFeeId =
                                                appListEntryFeeRepository.save(appListEntryFeeId);
                                            log.debug(
                                                "Created Fee: {} to Entry: {} mapping: {}",
                                                appListEntryFeeId.getFeeId(),
                                                appListEntryFeeId
                                                    .getAppListEntryId()
                                                    .getEntryFeeIds()
                                            );
                                        }

                                        return Optional.of(
                                                new AuditableResult<>(
                                                        MatchResponse.of(
                                                                listEntryEntity.getUuid(),
                                                                listEntryEntity,
                                                                entryGetDetailDto),
                                                        listEntryEntity));
                                    },
                                    auditLifecycleListeners.toArray(
                                            new AuditOperationLifecycleListener[0]));
                        });

        log.debug("Finish: Create Application Entry: {}", entryCreateDto);

        return getDetailDto;
    }
}
