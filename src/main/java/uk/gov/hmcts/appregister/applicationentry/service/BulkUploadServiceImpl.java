package uk.gov.hmcts.appregister.applicationentry.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationentry.dto.BulkUploadErrorDto;
import uk.gov.hmcts.appregister.applicationentry.dto.BulkUploadResponseDto;
import uk.gov.hmcts.appregister.applicationentry.dto.CsvRowDto;
import uk.gov.hmcts.appregister.applicationentry.util.Parser;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/** Service handling bulk upload of application entries via CSV files. */
@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadServiceImpl.class);

    private final Parser<CsvRowDto> csvParser;
    private final ApplicationListRepository listRepository;
    private final StandardApplicantRepository standardApplicantRepository;
    private final ApplicationCodeRepository applicationCodeRepository;

    private final ApplicationListEntrySaveService saveService;
    private final UserProvider userProvider;

    @Override
    public BulkUploadResponseDto uploadCsv(Long listId, MultipartFile file) {
        log.info("Bulk upload started for listId={} by user={}", listId, userProvider.getUser());
        ApplicationList list = findList(listId);

        List<CsvRowDto> rows = csvParser.parse(file);
        List<BulkUploadErrorDto> errors = new ArrayList<>();
        int validEntries = 0;

        for (int i = 0; i < rows.size(); i++) {
            CsvRowDto row = rows.get(i);
            int rowNumber = i + 2; // Account for header + 0-index

            try {
                StandardApplicant applicant = resolveStandardApplicant(row.standardApplicantCode());
                ApplicationCode applicationCode = resolveApplicationCode(row.applicationCode());
                ApplicationListEntry entry =
                        mapToEntity(row, list, applicant, applicationCode, userProvider.getUser());
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

        return new BulkUploadResponseDto(validEntries, errors);
    }

    private ApplicationList findList(Long listId) {
        return listRepository
                .findByIdAndCreatedUser(listId, userProvider.getUser())
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Application list not found or you do not have permission to access it."));
    }

    private StandardApplicant resolveStandardApplicant(String code) {
        return standardApplicantRepository
                .findByApplicantCode(code)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Standard applicant not found"));
    }

    private ApplicationCode resolveApplicationCode(String code) {
        List<ApplicationCode> applicationCodes =
                applicationCodeRepository.findByCodeAndDate(code, OffsetDateTime.now());
        if (applicationCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application code not found");
        }

        return applicationCodes.stream().findFirst().get();
    }

    private ApplicationListEntry mapToEntity(
            CsvRowDto row,
            ApplicationList list,
            StandardApplicant applicant,
            ApplicationCode applicationCode,
            String userId) {
        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setApplicationList(list);
        entry.setStandardApplicant(applicant);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber(row.accountNumber());
        entry.setApplicationListEntryWording(
                Stream.of(row.applicationText1(), row.applicationText2())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" ")));
        entry.setRnameaddress(buildIdentityDetails(row));
        entry.setNumberOfBulkRespondents(Short.valueOf(Short.MIN_VALUE));
        entry.setChangedDate(OffsetDateTime.now());
        entry.setBulkUpload("Y");
        entry.setEntryRescheduled("N");

        return entry;
    }

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
