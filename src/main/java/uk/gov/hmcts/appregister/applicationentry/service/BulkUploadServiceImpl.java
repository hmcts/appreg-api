package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationentry.dto.BulkUploadResponseDto;
import uk.gov.hmcts.appregister.applicationentry.dto.CsvRowDto;
import uk.gov.hmcts.appregister.applicationentry.util.Parser;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;

/** Service handling bulk upload of application entries via CSV files. */
@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

  private static final Logger log = LoggerFactory.getLogger(BulkUploadServiceImpl.class);

  private final Parser<CsvRowDto> csvParser;
  private final ApplicationListRepository listRepository;
  private final StandardApplicantRepository standardApplicantRepository;
  private final ApplicationCodeRepository applicationCodeRepository;

  // private final ApplicationSaveService saveService;

  @Override
  public BulkUploadResponseDto uploadCsv(Long listId, MultipartFile file, String userId) {
    /*log.info("Bulk upload started for listId={} by user={}", listId, userId);
    ApplicationList list = findList(listId, userId);

    List<CsvRowDto> rows = csvParser.parse(file);
    List<BulkUploadErrorDto> errors = new ArrayList<>();
    int validEntries = 0;

    for (int i = 0; i < rows.size(); i++) {
        CsvRowDto row = rows.get(i);
        int rowNumber = i + 2; // Account for header + 0-index

        try {
            StandardApplicant applicant =
                    resolveStandardApplicant(row.standardApplicantCode(), userId);
            ApplicationCode applicationCode = resolveApplicationCode(row.applicationCode());
            Application entry = mapToEntity(row, list, applicant, applicationCode, userId);
            saveService.saveApplication(entry);
            validEntries++;

            log.debug("Row {} saved successfully: {}", rowNumber, row);
        } catch (Exception e) {
            log.warn(
                    "Row {} failed: {} - {}",
                    rowNumber,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            log.debug("Failed row content: {}", row);
            errors.add(new BulkUploadErrorDto(rowNumber, row, e.getMessage()));
        }
    }

    return new BulkUploadResponseDto(validEntries, errors);*/
    return new BulkUploadResponseDto(1, List.of());
  }

  private ApplicationList findList(Long listId, String userId) {
    return listRepository
        .findByIdAndUserName(listId, userId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application list not found or you do not have permission to access it."));
  }

  private StandardApplicant resolveStandardApplicant(String code, String userId) {
    return standardApplicantRepository
        .findByApplicantCode(code)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Standard applicant not found"));
  }

  private ApplicationCode resolveApplicationCode(String code) {
    return applicationCodeRepository
        .findByApplicationCode(code)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application code not found"));
  }

  /* private Application mapToEntity(
          CsvRowDto row,
          ApplicationList list,
          StandardApplicant applicant,
          ApplicationCode applicationCode,
          String userId) {
      Application entry = new Application();
      entry.setApplicationList(list);
      entry.setStandardApplicant(applicant);
      entry.setApplicationCode(applicationCode);
      entry.setAccountNumber(row.accountNumber());
      entry.setApplicationWording(
              Stream.of(row.applicationText1(), row.applicationText2())
                      .filter(s -> s != null && !s.isBlank())
                      .collect(Collectors.joining(" ")));
      entry.setRespondent(buildIdentityDetails(row));
      entry.setNumberOfBulkRespondents(1);
      entry.setChangedBy(userId);
      entry.setChangedDate(LocalDate.now());
      entry.setBulkUpload("Y");
      entry.setApplicationRescheduled("N");
      entry.setVersion(1);

      return entry;
  }*/

  private NameAddress buildIdentityDetails(CsvRowDto row) {
    NameAddress identity = new NameAddress();
    identity.setTitle(row.respondentTitle());
    identity.setName(row.respondentOrganisationName());
    identity.setForename1(row.respondentForename1());
    identity.setForename2(row.respondentForename2());
    identity.setForename3(row.respondentForename3());
    identity.setSurname(row.respondentSurname());
    identity.setAddress1(row.respondentAddressLine1());
    identity.setAddress2(row.respondentAddressLine2());
    identity.setAddress3(row.respondentAddressLine3());
    identity.setAddress4(row.respondentAddressLine4());
    identity.setAddress5(row.respondentAddressLine5());
    identity.setPostcode(row.respondentPostcode());
    identity.setEmailAddress(row.respondentEmail());
    identity.setTelephoneNumber(row.respondentTelephone());
    identity.setMobileNumber(row.respondentMobile());
    return identity;
  }
}
