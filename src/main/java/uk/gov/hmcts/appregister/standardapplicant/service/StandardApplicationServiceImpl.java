package uk.gov.hmcts.appregister.standardapplicant.service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;
import uk.gov.hmcts.appregister.standardapplicant.mapper.CodeAndName;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;
import uk.gov.hmcts.appregister.standardapplicant.validator.StandardApplicantExistsValidator;

/**
 * Service implementation for managing standard applicants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StandardApplicationServiceImpl implements StandardApplicantService {
    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;
    private final Clock clock;
    private final ZoneId ukZone;
    private final PageMapper pageMapper;

    private final StandardApplicantExistsValidator validator;

    private final AuditOperationService auditService;
    private final ApplicantMapper applicantMapper;

    @Override
    public StandardApplicantPage findAll(
            String code,
            String name,
            String addressLine1,
            LocalDate from,
            LocalDate to,
            PagingWrapper pageable) {

        return auditService.processAudit(
                null,
                StandardApplicantOperation.GET_STANDARD_APPLICANTS,
                req -> {
                    // Use today's date to ensure we only return Result Codes that are currently
                    // active.
                    var todayUk = LocalDate.now(clock.withZone(ukZone));
                    var normalisedFrom = from;
                    var normalisedTo = to;

                    if (normalisedFrom != null
                            && normalisedTo != null
                            && normalisedFrom.isAfter(normalisedTo)) {
                        normalisedFrom = to;
                        normalisedTo = from;
                    }

                    // breaks name into individual and/or organisation parts
                    final Page<StandardApplicantEnrichedProjection> standardApplicantsList =
                            repository.search(
                                    code,
                                    name,
                                    addressLine1,
                                    normalisedFrom,
                                    normalisedTo,
                                    todayUk,
                                    pageable.getPageable());

                    StandardApplicantPage newPage = new StandardApplicantPage();
                    pageMapper.toPage(standardApplicantsList, newPage, pageable.getSortStrings());

                    // Map each projection to a summary DTO and add to the page content
                    standardApplicantsList.forEach(
                            projection ->
                                    newPage.addContentItem(mapper.toReadGetSummaryDto(projection)));

                    log.debug(
                            "Finished: Find Standard Applicant for: code: {} name: {} with paging: {}",
                            code,
                            name,
                            pageable);

                    CodeAndName codeAndName =
                            new CodeAndName(code, name, addressLine1, normalisedFrom, normalisedTo);
                    AuditableResult<StandardApplicantPage, StandardApplicant> result =
                            new AuditableResult<>(newPage, mapper.toEntity(codeAndName));

                    return Optional.of(result);
                });
    }

    @Override
    public StandardApplicantGetDetailDto findByCode(String code) {
        return auditService.processAudit(
                null,
                StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE,
                req -> findByCodeAuditResult(code));
    }

    @Override
    public String generateCsv(String code, String name) {

        if ((code != null && name != null) || (code == null && name == null)) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.CODE_AND_NAME_EXCLUSION_VIOLATION,
                    "Unable to generate CSV for Standard Applicants. At least one of code or name must be provided.");
        }

        List<StandardApplicant> filteredList = repository.findByCodeAndName(code, name);

        if (filteredList.isEmpty()) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.NO_RESULTS_FOUND_FOR_CSV_GENERATION,
                    "Unable to generate CSV for Standard Applicants. No records found for the provided code or name.");
        }

        try {
            try (CsvWriter<StandardApplicantCsvRow> writer =
                    new CsvWriter<>(StandardApplicantCsvRow.class)) {
                return writer.writeToString(
                        mapper.toEntity(filteredList),
                        StandardApplicantCsvRow.class,
                        StandardApplicantCsvRow.Header);
            }
        } catch (IOException io) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.CANNOT_GENERATE_CSV,
                    "Unable to generate CSV for Standard Applicants.");
        }
    }

    private Optional<AuditableResult<StandardApplicantGetDetailDto, StandardApplicant>>
            findByCodeAuditResult(String code) {
        log.debug("Start: Find Standard Applicant By Code for: app code: {}", code);

        var result =
                validator.validate(
                        code,
                        (requestedCode, standardApplicant) ->
                                new AuditableResult<>(
                                        mapper.toReadGetDto(standardApplicant),
                                        mapper.toEntity(requestedCode)));

        log.debug("Finish: Find Standard Applicant By Code for: app code: {}", code);
        return Optional.of(result);
    }
}
