package uk.gov.hmcts.appregister.applicationlistentryresolutions.service;

import java.util.Optional;
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
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.validator.ValidationExceptionHandler;

/**
 * Service implementation for managing application results.
 */
@Service
@RequiredArgsConstructor
public class ApplicationListEntryResolutionServiceImpl
        implements ApplicationListEntryResolutionService {

    private final WordingTemplateParser parser;

    private final ResolutionCodeRepository resolutionCodeRepository;
    private final ApplicationListEntryResolutionMapper mapper;
    private final AppListEntryResolutionRepository appListEntryResolutionRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final UserProvider userProvider;

    @Override
    @Transactional(readOnly = true)
    public ApplicationListEntryResolutionDto getResultForApplication(
            Long listId, Long applicationId) {
        AppListEntryResolution result = findOrThrow(null, applicationId, listId);
        return mapper.toReadDto(result);
    }

    @Override
    @Transactional
    public ApplicationListEntryResolutionDto create(
            Long listId, Long applicationId, ApplicationListEntryResolutionWriteDto dto) {
        ApplicationListEntry app =
                applicationListEntryRepository
                        .findByIdAndCreatedUser(applicationId, userProvider.getUserId())
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

        AppListEntryResolution result = mapper.createFromWriteDto(dto, wording);
        result.setApplicationList(app);

        Optional<ResolutionCode> res =
                resolutionCodeRepository.findByResultCode(resultCode.getResultCode());

        res.ifPresent(result::setResolutionCode);

        return mapper.toReadDto(appListEntryResolutionRepository.save(result));
    }

    @Override
    @Transactional
    public ApplicationListEntryResolutionDto update(
            Long listId,
            Long applicationId,
            Long resultId,
            ApplicationListEntryResolutionWriteDto dto) {
        AppListEntryResolution existing = findOrThrow(resultId, applicationId, listId);

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

        Optional<ResolutionCode> resolutionCode =
                resolutionCodeRepository.findByResultCode(resultCode.getResultCode());

        resolutionCode.ifPresent(existing::setResolutionCode);

        mapper.updateFromWriteDto(dto, existing, wording);

        return mapper.toReadDto(appListEntryResolutionRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long listId, Long applicationId, Long resultId) {
        AppListEntryResolution result = findOrThrow(resultId, applicationId, listId);
        appListEntryResolutionRepository.delete(result);
    }

    private AppListEntryResolution findOrThrow(Long resultId, Long appId, Long listId) {
        if (resultId != null) {
            return appListEntryResolutionRepository
                    .findByIdWithApplicationAndListAndCreatedUser(
                            resultId, appId, listId, userProvider.getUserId())
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND, "Result not found"));
        }

        return appListEntryResolutionRepository
                .findById(appId)
                .filter(r -> r.getApplicationList().getId().equals(listId))
                .filter(
                        r ->
                                r.getApplicationList()
                                        .getCreatedUser()
                                        .equals(userProvider.getUserId()))
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Result not found"));
    }
}
