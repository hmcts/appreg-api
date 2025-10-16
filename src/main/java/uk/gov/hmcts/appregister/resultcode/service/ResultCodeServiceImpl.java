package uk.gov.hmcts.appregister.resultcode.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.time.DateBoundaryCalculator;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
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
@Transactional(readOnly = true)
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

    private final DateBoundaryCalculator dateBoundaryCalculator;

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
    public ResultCodeGetDetailDto findByCodeAndDate(String code, LocalDate date) {
        return auditService.processAudit(
                AuditEventEnum.GET_RESULT_CODE_AUDIT_EVENT,
                unused -> {
                    var dateStart = dateBoundaryCalculator.startOfDay(date);
                    var dateEnd = dateBoundaryCalculator.startOfNextDay(date);
                    final List<ResolutionCode> rows =
                            repository.findActiveResolutionCodesOnDateByCode(
                                    code, dateStart, dateEnd);

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

                    // Map the single matching entity to a detail DTO
                    return Optional.of(mapper.toDetailDto(rows.getFirst()));
                },
                // Spring injects all AuditOperationLifecycleListener beans as a List;
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
    public ResultCodePage getPage(String codeFilter, String titleFilter, Pageable pageable) {
        return auditService.processAudit(
                AuditEventEnum.GET_RESULT_CODES_AUDIT_EVENT,
                unused -> {
                    var ukToday = dateBoundaryCalculator.getToday();
                    var startOfToday = dateBoundaryCalculator.startOfDay(ukToday);
                    var startOfTomorrow = dateBoundaryCalculator.startOfNextDay(ukToday);

                    Page<ResolutionCode> dbPage =
                            repository.findActiveOnDate(
                                    codeFilter,
                                    titleFilter,
                                    startOfToday,
                                    startOfTomorrow,
                                    pageable);

                    // Populate the API page response with metadata
                    var responsePage = new ResultCodePage();

                    // Ensure content is never null: API spec requires an array, so return []
                    // instead of null
                    if (responsePage.getContent() == null) {
                        responsePage.setContent(new ArrayList<>());
                    }

                    pageMapper.toPage(dbPage, responsePage);

                    // Map each entity to a summary DTO and add to the page content
                    dbPage.forEach(code -> responsePage.addContentItem(mapper.toSummaryDto(code)));

                    return Optional.of(responsePage);
                },
                // Spring injects all AuditOperationLifecycleListener beans as a List;
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
