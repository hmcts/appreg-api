package uk.gov.hmcts.appregister.standardapplicant.service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant_;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintSearchCriteriaDto;
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
    private static final int PRINT_PAGE_SIZE = 1000;
    private static final int MAX_PRINT_ROWS = 1000;

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
                    newPage.setContent(new ArrayList<>());

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
    public StandardApplicantPrintDto print(
            String code,
            String name,
            String addressLine1,
            LocalDate from,
            LocalDate to,
            PagingWrapper pageable) {

        return auditService.processAudit(
                null,
                StandardApplicantOperation.PRINT_STANDARD_APPLICANTS,
                req -> {
                    var todayUk = LocalDate.now(clock.withZone(ukZone));
                    var normalisedFrom = from;
                    var normalisedTo = to;

                    if (normalisedFrom != null
                            && normalisedTo != null
                            && normalisedFrom.isAfter(normalisedTo)) {
                        normalisedFrom = to;
                        normalisedTo = from;
                    }

                    var applicants = new ArrayList<StandardApplicantEnrichedProjection>();
                    var printPageable =
                            PageRequest.of(
                                    0,
                                    1000,
                                    withStableTieBreaker(pageable.getPageable().getSort()));

                    Page<StandardApplicantEnrichedProjection> results;
                    do {
                        results =
                                repository.search(
                                        code,
                                        name,
                                        addressLine1,
                                        normalisedFrom,
                                        normalisedTo,
                                        todayUk,
                                        printPageable);

                        if (results.getTotalElements() > MAX_PRINT_ROWS) {
                            throw new AppRegistryException(
                                    StandardApplicantCodeError.PRINT_RESULT_LIMIT_EXCEEDED,
                                    "Standard Applicant print result exceeds %d rows; narrow the search criteria"
                                            .formatted(MAX_PRINT_ROWS));
                        }

                        applicants.addAll(results.getContent());
                        printPageable = printPageable.next();
                    } while (results.hasNext());

                    var criteria =
                            new StandardApplicantPrintSearchCriteriaDto()
                                    .code(code)
                                    .name(name)
                                    .addressLine1(addressLine1)
                                    .from(normalisedFrom)
                                    .to(normalisedTo);

                    var dto =
                            new StandardApplicantPrintDto()
                                    .reportTitle("Standard Applicants Report")
                                    .generatedAt(OffsetDateTime.now(clock.withZone(ukZone)))
                                    .recordCount(applicants.size())
                                    .searchCriteria(criteria)
                                    .applicants(
                                            applicants.stream()
                                                    .map(mapper::toPrintRowDto)
                                                    .toList());

                    CodeAndName codeAndName =
                            new CodeAndName(code, name, addressLine1, normalisedFrom, normalisedTo);
                    AuditableResult<StandardApplicantPrintDto, StandardApplicant> result =
                            new AuditableResult<>(dto, mapper.toEntity(codeAndName));

                    return Optional.of(result);
                });
    }

    public String generateCsv(String code, String name) {

        boolean codeProvided = code != null && !code.isBlank();
        boolean nameProvided = name != null && !name.isBlank();

        if ((codeProvided && nameProvided) || (!codeProvided && !nameProvided)) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.CODE_AND_NAME_EXCLUSION_VIOLATION,
                    "Unable to generate CSV for Standard Applicants. At least one of code or name must be provided.");
        }

        // Need to make sure if one value is blank that we pass null through instead of the blank
        // value.
        List<StandardApplicant> filteredList =
                repository.findByCodeAndName(
                        codeProvided ? code : null, nameProvided ? name : null);

        if (filteredList.isEmpty()) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.NO_RESULTS_FOUND_FOR_CSV_GENERATION,
                    "Unable to generate CSV for Standard Applicants. No records found for the provided code or name.");
        }

        try {
            try (CsvWriter<StandardApplicantCsvRow> writer =
                    new CsvWriter<>(StandardApplicantCsvRow.class)) {
                var rows = mapper.toEntity(filteredList);
                rows.forEach(
                        row -> {
                            row.setApplicantCode(CsvUtil.escapeCharacters(row.getApplicantCode()));
                            row.setName(CsvUtil.escapeCharacters(row.getName()));
                        });

                return writer.writeToString(
                        rows, StandardApplicantCsvRow.class, StandardApplicantCsvRow.Header);
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

    private static Sort withStableTieBreaker(Sort sort) {
        var existingSort = sort == null ? Sort.unsorted() : sort;

        if (hasSortProperty(existingSort, StandardApplicant_.ID)) {
            return existingSort;
        }

        return existingSort.and(Sort.by(Sort.Direction.ASC, StandardApplicant_.ID));
    }

    private static boolean hasSortProperty(Sort sort, String property) {
        return sort.stream().anyMatch(order -> property.equals(order.getProperty()));
    }
}
