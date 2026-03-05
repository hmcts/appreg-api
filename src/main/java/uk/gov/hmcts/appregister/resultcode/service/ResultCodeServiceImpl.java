package uk.gov.hmcts.appregister.resultcode.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.audit.ResultCodeOperation;
import uk.gov.hmcts.appregister.resultcode.exception.ResultCodeError;
import uk.gov.hmcts.appregister.resultcode.mapper.ResultCodeMapper;

/**
 * Service implementation for Result Code operations.
 *
 * <p>Provides business logic for retrieving Result Codes by delegating to the {@link
 * ResultCodeService} and mapping entities into API DTOs. All operations are executed within an
 * audited context using {@link AuditOperationService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResultCodeServiceImpl implements ResultCodeService {

    private static final int SINGLE_RECORD = 1;

    // Service for wrapping operations in an auditable context.
    private final AuditOperationService auditService;

    // Lifecycle listeners invoked during audit processing.
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    // Repository for querying {@link ResolutionCode} entities.
    private final ResolutionCodeRepository repository;

    // Mapper converting {@link ResolutionCode} entities to API DTOs.
    private final ResultCodeMapper mapper;

    // Mapper for transferring Spring Data {@link Page} metadata into API page objects.
    private final PageMapper pageMapper;

    // Clock used to obtain the current time.
    private final Clock clock;

    // Zone identifier representing the UK business timezone (Europe/London).
    private final ZoneId ukZone;

    /**
     * Retrieve a Result Code by its code and effective date.
     *
     * <p>Ensures only one active record exists for the given combination of {@code code} and {@code
     * date}. Throws a domain-specific exception if no record or multiple records are found.
     *
     * @param code the identifier for the Result Code (case-insensitive)
     * @param date ISO date on which the Result Code must be valid
     * @return a detailed Result Code DTO
     * @throws AppRegistryException if no match or multiple matches are found
     */
    @Override
    public ResultCodeGetDetailDto findByCode(String code, LocalDate date) {
        return auditService.processAudit(
                ResultCodeOperation.GET_RESULT_CODE_AUDIT_EVENT,
                unused -> {
                    log.debug("Start: Find active Result Code using code: {} date: {}", code, date);
                    final List<ResolutionCode> rows =
                            repository.findActiveResolutionCodesByCodeAndDate(code, date);

                    if (rows.isEmpty()) {
                        throw new AppRegistryException(
                                ResultCodeError.RESULT_CODE_NOT_FOUND,
                                "No result code found for code '%s' on date %s"
                                        .formatted(code, date));
                    } else if (rows.size() > SINGLE_RECORD) {
                        throw new AppRegistryException(
                                ResultCodeError.DUPLICATE_RESULT_CODE_FOUND,
                                "Multiple result codes found for code '%s' on date %s"
                                        .formatted(code, date));
                    }

                    log.debug(
                            "Finish: Find active Result Code for code: {} on date: {}", code, date);

                    return Optional.of(
                            new AuditableResult<ResultCodeGetDetailDto, Keyable>(
                                    mapper.toDetailDto(rows.getFirst()),
                                    mapper.toEntity(code, date)));
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    /**
     * Retrieve a paginated list of active Result Codes.
     *
     * <p>Supports optional filters on code and name, case-insensitive partial matches, and maps the
     * entity results into summary DTOs with page metadata.
     *
     * @param codeFilter optional partial filter on code
     * @param titleFilter optional partial filter on title
     * @param pageable pagination and sorting configuration
     * @return a page of Result Code summaries
     */
    @Override
    public ResultCodePage findAll(String codeFilter, String titleFilter, PagingWrapper pageable) {

        // Use today's date to ensure we only return Result Codes that are currently active.
        var todayUk = LocalDate.now(clock.withZone(ukZone));

        return auditService.processAudit(
                ResultCodeOperation.GET_RESULT_CODES_AUDIT_EVENT,
                unused -> {
                    log.debug(
                            "Start: Find active Result Codes filtered by code: {} and title: {}",
                            codeFilter,
                            titleFilter);

                    Page<ResolutionCode> dbPage =
                            repository.findActiveOnDate(
                                    codeFilter, titleFilter, todayUk, pageable.getPageable());

                    var responsePage = new ResultCodePage();

                    if (responsePage.getContent() == null) {
                        responsePage.setContent(new ArrayList<>());
                    }

                    pageMapper.toPage(dbPage, responsePage, pageable.getSortStrings());

                    // Map each entity to a summary DTO and add to the page content
                    dbPage.forEach(rc -> responsePage.addContentItem(mapper.toSummaryDto(rc)));

                    log.debug(
                            "Finish: Find active Result Codes filtered by code: {} and title: {}",
                            codeFilter,
                            titleFilter);
                    AuditableResult<ResultCodePage, Keyable> result;
                    if (codeFilter == null && titleFilter == null) {
                        result = new AuditableResult<>(responsePage, mapper.toEntity("", ""));
                    } else {
                        result =
                                new AuditableResult<>(
                                        responsePage, mapper.toEntity(codeFilter, titleFilter));
                    }
                    return Optional.of(result);
                },
                // Spring injects all AuditOperationLifecycleListener beans as a List;
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
