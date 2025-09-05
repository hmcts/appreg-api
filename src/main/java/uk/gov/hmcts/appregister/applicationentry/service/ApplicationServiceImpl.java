package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;

/** Service Implementation for managing Applications. */
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    /*  private final WordingTemplateParser parser;
        private final ApplicationMapper applicationMapper;
        private final ApplicationFeeRecordMapper feeRecordMapper;
        private final ApplicationRepository applicationRepository;
        private final ApplicationListRepository listRepository;
        private final StandardApplicantRepository standardApplicantRepository;
        private final ApplicationCodeRepository applicationCodeRepository;
        private final ApplicationFeeService feeService;
    */
    @Override
    public List<ApplicationDto> getAllByListId(Long listId, String userId) {
        // ensureUserOwnsList(listId, userId);

        // TODO: Fix me
        // List<Application> applications =
        // applicationRepository.findByApplicationListIdWithJoins(listId);
        // return applications.stream().map(this::toDtoWithFees).toList();
        return List.of();
    }

    @Override
    public ApplicationDto getByIdForUser(Long listId, Long appId, String userId) {
        // TODO: Fix me
        // return toDtoWithFees(getApplicationForUserOrThrow(listId, appId, userId));

        return null;
    }

    @Override
    @Transactional
    public ApplicationDto create(Long listId, ApplicationWriteDto dto, String userId) {
        /*     ApplicationList list = findListOrThrow(listId, userId);
                StandardApplicant applicant = resolveStandardApplicant(dto.standardApplicantId());
                ApplicationCode code = findApplicationCodeOrThrow(dto.applicationCodeId());
                String wording = generateWording(code, dto);
                LocalDate changedDate = LocalDate.now();
        */
        // TODO: Fix me
        /*
            Application app =
                    applicationMapper.createFromWriteDto(
                            dto, applicant, wording, code, userId, changedDate);
            app.setApplicationList(list);

            attachFeeRecords(app, code, dto, userId, changedDate);

            Application saved = applicationRepository.save(app);
            return applicationMapper.toReadDto(
                    saved, feeService.resolveFeePair(code.getFeeReference()));
        */

        return null;
    }

    @Override
    @Transactional
    public ApplicationDto update(Long listId, Long appId, ApplicationWriteDto dto, String userId) {
        // TODO: Fix me
        /* Application existing = getApplicationForUserOrThrow(listId, appId, userId);
        StandardApplicant applicant = resolveStandardApplicant(dto.standardApplicantId());
        ApplicationCode code = findApplicationCodeOrThrow(dto.applicationCodeId());
        String wording = generateWording(code, dto);
        LocalDate changedDate = LocalDate.now();

        applicationMapper.updateFromWriteDto(
                dto, existing, applicant, wording, code, userId, changedDate);

        existing.getFeeRecords().clear();
        attachFeeRecords(existing, code, dto, userId, changedDate);

        Application saved = applicationRepository.save(existing);
        return applicationMapper.toReadDto(
                saved, feeService.resolveFeePair(code.getFeeReference()));*/

        return null;
    }

    @Override
    public void delete(Long listId, Long appId, String userId) {
        /*        Application app = getApplicationForUserOrThrow(listId, appId, userId);
        applicationRepository.delete(app);*/
    }

    /*
      private void attachFeeRecords(
              Application app,
              ApplicationCode code,
              ApplicationWriteDto dto,
              String userId,
              LocalDate changedDate) {
          if (!Boolean.TRUE.equals(code.getFeeDue())) {
              return;
          }

          FeePair feePair = feeService.resolveFeePair(code.getFeeReference());

          if (feePair.mainFee() != null) {
              app.addFeeRecord(
                      feeRecordMapper.createEntity(dto, app, feePair.mainFee(), userId, changedDate));
          }

          if (Boolean.TRUE.equals(dto.includesOffsetPayment()) && feePair.offsetFee() != null) {
              app.addFeeRecord(
                      feeRecordMapper.createEntity(
                              dto, app, feePair.offsetFee(), userId, changedDate));
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
                  .findByIdAndUserId(listId, userId)
                  .orElseThrow(
                          () ->
                                  new ResponseStatusException(
                                          HttpStatus.NOT_FOUND, "Application List not found"));
      }

      private Application getApplicationForUserOrThrow(Long listId, Long appId, String userId) {
          return applicationRepository
                  .findByIdAndApplicationListIdAndApplicationListUserId(appId, listId, userId)
                  .orElseThrow(
                          () ->
                                  new ResponseStatusException(
                                          HttpStatus.NOT_FOUND,
                                          "Application not found or not accessible"));
      }

      private void ensureUserOwnsList(Long listId, String userId) {
          if (!listRepository.existsByIdAndUserId(listId, userId)) {
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

      private ApplicationDto toDtoWithFees(Application app) {
          FeePair fees = feeService.resolveFeePair(app.getApplicationCode().getFeeReference());
          return applicationMapper.toReadDto(app, fees);
      }
    */
}
