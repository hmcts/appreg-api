package uk.gov.hmcts.appregister.applicationlistentryresolutions.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationentry.util.WordingTemplateParser;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionWriteDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.mapper.ApplicationListEntryResolutionMapper;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.entity.security.UserProvider;
import uk.gov.hmcts.appregister.exception.ValidationExceptionHandler;

// import uk.gov.hmcts.appregister.applicationresult.mapper.ApplicationResultMapper;
// import uk.gov.hmcts.appregister.applicationresult.repository.ApplicationResultRepository;

/** Service implementation for managing application results. */
@Service
@RequiredArgsConstructor
public class ApplicationListEntryResolutionServiceImpl
        implements ApplicationListEntryResolutionService {

    private final WordingTemplateParser parser;

    private final ApplicationListRepository applicationListRepository;
    private final ResolutionCodeRepository resolutionCodeRepository;
    private final ApplicationListEntryResolutionMapper mapper;
    private final AppListEntryResolutionRepository appListEntryResolutionRepository;
    private final UserProvider authenticatedUser;
    private final ApplicationListEntryRepository applicationListEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public ApplicationListEntryResolutionDto getResultForApplication(
            Long listId, Long applicationId, String userId) {
        AppListEntryResolution result = findOrThrow(null, applicationId, listId, userId);
        return mapper.toReadDto(result);
    }

    @Override
    @Transactional
    public ApplicationListEntryResolutionDto create(
            Long listId,
            Long applicationId,
            ApplicationListEntryResolutionWriteDto dto,
            String userId) {
        ApplicationListEntry app =
                applicationListEntryRepository
                        .findByIdAndCreatedUser(applicationId, userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Application not found"));

        ResolutionCode resultCode =
                resolutionCodeRepository
                        .findById(dto.resultCodeId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Result code not found"));

        String wording =
                ValidationExceptionHandler.wrap(
                        () -> parser.generateWording(resultCode.getWording(), dto.textFields()));

        AppListEntryResolution result =
                mapper.createFromWriteDto(dto, userId, LocalDate.now(), wording);
        result.setApplicationList(app);

        result.setResolutionCode(
                resolutionCodeRepository.findByResultCode(resultCode.getResultCode()).get());

        return mapper.toReadDto(appListEntryResolutionRepository.save(result));
    }

    @Override
    @Transactional
    public ApplicationListEntryResolutionDto update(
            Long listId,
            Long applicationId,
            Long resultId,
            ApplicationListEntryResolutionWriteDto dto) {
        AppListEntryResolution existing =
                findOrThrow(resultId, applicationId, listId, authenticatedUser.getUser());

        ResolutionCode resultCode =
                resolutionCodeRepository
                        .findById(dto.resultCodeId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Result code not found"));

        String wording =
                ValidationExceptionHandler.wrap(
                        () -> parser.generateWording(resultCode.getWording(), dto.textFields()));

        existing.setResolutionCode(
                resolutionCodeRepository.findByResultCode(resultCode.getResultCode()).get());

        mapper.updateFromWriteDto(dto, existing, wording);

        return mapper.toReadDto(appListEntryResolutionRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long listId, Long applicationId, Long resultId, String userId) {
        AppListEntryResolution result = findOrThrow(resultId, applicationId, listId, userId);
        appListEntryResolutionRepository.delete(result);
    }

    private AppListEntryResolution findOrThrow(
            Long resultId, Long appId, Long listId, String userId) {
        if (resultId != null) {
            return appListEntryResolutionRepository
                    .findByIdWithApplicationAndListAndCreatedUser(resultId, appId, listId, userId)
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND, "Result not found"));
        }

        return appListEntryResolutionRepository
                .findById(appId)
                .filter(r -> r.getApplicationList().getId().equals(listId))
                .filter(r -> r.getApplicationList().getCreatedUser().equals(userId))
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Result not found"));
    }
}
