package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationListEntryDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;
import uk.gov.hmcts.appregister.applicationentry.mapper.AppListEntryFeeStatusMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.util.WordingTemplateParser;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.exception.ValidationExceptionHandler;

/** Service Implementation for managing Applications. */
@Service
@RequiredArgsConstructor
public class ApplicationListEntryServiceImpl implements ApplicationListEntryService {

    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final ApplicationListRepository listRepository;
    private final WordingTemplateParser parser;
    private final ApplicationCodeRepository applicationCodeRepository;
    private final StandardApplicantRepository standardApplicantRepository;
    private final ApplicationFeeService feeService;
    private final ApplicationListEntryMapper applicationMapper;
    private final AppListEntryFeeStatusMapper feeRecordMapper;
    private final AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    @Override
    public List<ApplicationListEntryDto> getAllByListId(Long listId, String userId) {
        ensureUserOwnsList(listId, userId);

        List<ApplicationListEntry> applications =
                applicationListEntryRepository.findByApplicationListIdAndCreatedUser(
                        listId, userId);
        return applications.stream().map(this::toDtoWithFees).toList();
    }

    @Override
    public ApplicationListEntryDto getByIdForUser(Long listId, Long appId, String userId) {
        return toDtoWithFees(getApplicationForUserOrThrow(listId, appId, userId));
    }

    @Override
    @Transactional
    public ApplicationListEntryDto create(Long listId, ApplicationWriteDto dto, String userId) {
        ApplicationList list = findListOrThrow(listId, userId);
        StandardApplicant applicant = resolveStandardApplicant(dto.standardApplicantId());
        ApplicationCode code = findApplicationCodeOrThrow(dto.applicationCodeId());
        String wording = generateWording(code, dto);

        ApplicationListEntry app =
                applicationMapper.createFromWriteDto(dto, applicant, wording, code);
        app.setApplicationList(list);

        attachFeeRecords(app, code, dto);

        ApplicationListEntry saved = applicationListEntryRepository.save(app);
        return applicationMapper.toReadDto(
                saved, feeService.resolveFeePair(code.getFeeReference()));
    }

    @Override
    @Transactional
    public ApplicationListEntryDto update(
            Long listId, Long appId, ApplicationWriteDto dto, String userId) {
        ApplicationListEntry existing = getApplicationForUserOrThrow(listId, appId, userId);
        StandardApplicant applicant = resolveStandardApplicant(dto.standardApplicantId());
        ApplicationCode code = findApplicationCodeOrThrow(dto.applicationCodeId());
        String wording = generateWording(code, dto);

        applicationMapper.updateFromWriteDto(dto, existing, applicant, wording, code);

        existing.getEntryFeeStatuses().clear();
        attachFeeRecords(existing, code, dto);

        ApplicationListEntry saved = applicationListEntryRepository.save(existing);
        return applicationMapper.toReadDto(
                saved, feeService.resolveFeePair(code.getFeeReference()));
    }

    @Override
    public void delete(Long listId, Long appId, String userId) {
        ApplicationListEntry app = getApplicationForUserOrThrow(listId, appId, userId);
        applicationListEntryRepository.delete(app);
    }

    private void attachFeeRecords(
            ApplicationListEntry app, ApplicationCode code, ApplicationWriteDto dto) {
        if (!Boolean.TRUE.equals(code.getFeeDue())) {
            return;
        }

        FeePair feePair = feeService.resolveFeePair(code.getFeeReference());

        if (feePair.mainFee() != null) {
            app.getEntryFeeStatuses()
                    .add(
                            appListEntryFeeStatusRepository.save(
                                    feeRecordMapper.createEntity(dto, app, feePair.mainFee())));
        }

        if (Boolean.TRUE.equals(dto.includesOffsetPayment()) && feePair.offsetFee() != null) {
            app.getEntryFeeStatuses()
                    .add(
                            appListEntryFeeStatusRepository.save(
                                    feeRecordMapper.createEntity(dto, app, feePair.offsetFee())));
        }
    }

    private String generateWording(ApplicationCode code, ApplicationWriteDto dto) {
        return ValidationExceptionHandler.wrap(
                () -> parser.generateWording(code.getWording(), dto.textFields()));
    }

    private ApplicationCode findApplicationCodeOrThrow(Long id) {
        return applicationCodeRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Application code not found"));
    }

    private ApplicationList findListOrThrow(Long listId, String userId) {
        return listRepository
                .findByIdAndCreatedUser(listId, userId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Application List not found"));
    }

    private ApplicationListEntry getApplicationForUserOrThrow(
            Long listId, Long appId, String userId) {
        return applicationListEntryRepository
                .findByIdAndApplicationListIdAndCreatedUser(appId, listId, userId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Application not found or not accessible"));
    }

    private void ensureUserOwnsList(Long listId, String userId) {
        if (!listRepository.existsByIdAndCreatedUser(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found for user");
        }
    }

    private StandardApplicant resolveStandardApplicant(Long standardApplicantId) {
        if (standardApplicantId == null) {
            return null;
        }
        return standardApplicantRepository
                .findById(standardApplicantId)
                .filter(a -> a.getApplicantEndDate() == null)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Standard applicant is inactive or not found"));
    }

    private ApplicationListEntryDto toDtoWithFees(ApplicationListEntry app) {
        FeePair fees = feeService.resolveFeePair(app.getApplicationCode().getFeeReference());
        return applicationMapper.toReadDto(app, fees);
    }
}
