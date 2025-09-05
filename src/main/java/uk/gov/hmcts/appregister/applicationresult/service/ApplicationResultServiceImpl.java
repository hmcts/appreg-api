package uk.gov.hmcts.appregister.applicationresult.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentry.util.WordingTemplateParser;
import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultDto;
import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultWriteDto;

// import uk.gov.hmcts.appregister.applicationresult.mapper.ApplicationResultMapper;
// import uk.gov.hmcts.appregister.applicationresult.repository.ApplicationResultRepository;

/** Service implementation for managing application results. */
@Service
@RequiredArgsConstructor
public class ApplicationResultServiceImpl implements ApplicationResultService {

  private final WordingTemplateParser parser;

  /*  private final ApplicationResultRepository resultRepository;
      private final ApplicationRepository applicationRepository;
      private final ResolutionCodeRepository resolutionCodeRepository;
      private final ApplicationResultMapper mapper;
      private final VersionManager versionManager;
  */
  @Override
  @Transactional(readOnly = true)
  public ApplicationResultDto getResultForApplication(
      Long listId, Long applicationId, String userId) {
    // TODO: Fix me
    /*   ApplicationResult result = findOrThrow(null, applicationId, listId, userId);
    return mapper.toReadDto(result);*/
    return null;
  }

  @Override
  @Transactional
  public ApplicationResultDto create(
      Long listId, Long applicationId, ApplicationResultWriteDto dto, String userId) {
    /* Application app =
            applicationRepository
                    .findByIdAndApplicationListIdAndApplicationListUserId(
                            applicationId, listId, userId)
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

    ApplicationResult result = mapper.createFromWriteDto(dto, userId, LocalDate.now(), wording);

    result.setResultCode(resultCode);
    result.setApplication(app);

    return mapper.toReadDto(resultRepository.save(result));*/
    return null;
  }

  @Override
  @Transactional
  public ApplicationResultDto update(
      Long listId,
      Long applicationId,
      Long resultId,
      ApplicationResultWriteDto dto,
      String userId) {
    /*   ApplicationResult existing = findOrThrow(resultId, applicationId, listId, userId);

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

    mapper.updateFromWriteDto(dto, existing, userId, LocalDate.now(), wording);
    existing.setResultCode(resultCode);
    existing.setVersion(versionManager.increment(existing.getVersion()));

    return mapper.toReadDto(resultRepository.save(existing));*/

    return null;
  }

  @Override
  @Transactional
  public void delete(Long listId, Long applicationId, Long resultId, String userId) {
    /*        ApplicationResult result = findOrThrow(resultId, applicationId, listId, userId);
    resultRepository.delete(result);*/

  }

  // TODO: Fix me
  /*private ApplicationResult findOrThrow(Long resultId, Long appId, Long listId, String userId) {
      if (resultId != null) {
          return resultRepository
                  .findByIdWithApplicationAndListAndUser(resultId, appId, listId, userId)
                  .orElseThrow(
                          () ->
                                  new ResponseStatusException(
                                          HttpStatus.NOT_FOUND, "Result not found"));
      }

      return resultRepository
              .findByApplicationId(appId)
              .filter(r -> r.getApplication().getApplicationList().getId().equals(listId))
              .filter(r -> r.getApplication().getApplicationList().getUserId().equals(userId))
              .orElseThrow(
                      () ->
                              new ResponseStatusException(
                                      HttpStatus.NOT_FOUND, "Result not found"));
  }*/
}
