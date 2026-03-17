package uk.gov.hmcts.appregister.courtlocation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.courtlocation.audit.CourtLocationAuditOperation;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.courtlocation.mapper.CodeAndName;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapper;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

/**
 * Service implementation for Court Location operations.
 *
 * <p>Provides business logic for retrieving Court Locations by delegating to the {@link
 * NationalCourtHouseRepository} and mapping entities into API DTOs. All operations are executed
 * within an audited context using {@link AuditOperationService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourtLocationServiceImpl implements CourtLocationService {

    private static final int SINGLE_RECORD = 1;

    // Service for wrapping operations in an auditable context.
    private final AuditOperationService auditService;

    // Lifecycle listeners invoked during audit processing.
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    // Repository for querying {@link NationalCourtHouse} entities.
    private final NationalCourtHouseRepository repository;

    // Mapper converting {@link NationalCourtHouse} entities to API DTOs.
    private final CourtLocationMapper mapper;

    // Mapper for transferring Spring Data {@link Page} metadata into API page objects.
    private final PageMapper pageMapper;

    /**
     * Retrieve a Court Location by its code and effective date.
     *
     * <p>Ensures only one active record exists for the given combination of {@code code} and {@code
     * date}. Throws a domain-specific exception if no record or multiple records are found.
     *
     * @param code the business identifier for the Court Location (case-insensitive)
     * @param date ISO date on which the Court Location must be valid
     * @return a detailed Court Location DTO
     * @throws AppRegistryException if no match or multiple matches are found
     */
    @Override
    @Transactional(readOnly = true)
    public CourtLocationGetDetailDto findByCodeAndDate(String code, LocalDate date) {
        return auditService.processAudit(
                CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT,
                unused -> {
                    final List<NationalCourtHouse> rows =
                            repository.findActiveCourtsWithDate(code, date);

                    if (rows.isEmpty()) {
                        throw new AppRegistryException(
                                CourtLocationError.COURT_NOT_FOUND,
                                "No court found for code '%s' on date %s".formatted(code, date));
                    } else if (rows.size() > SINGLE_RECORD) {
                        throw new AppRegistryException(
                                CourtLocationError.DUPLICATE_COURT_FOUND,
                                "Multiple courts found for code '%s' on date %s"
                                        .formatted(code, date));
                    }

                    AuditableResult<CourtLocationGetDetailDto, NationalCourtHouse> result =
                            new AuditableResult<>(
                                    mapper.toDetailDto(rows.getFirst()),
                                    mapper.toEntity(code, date));

                    // Map the single matching entity to a detail DTO
                    return Optional.of(result);
                },
                // Spring injects all AuditOperationLifecycleListener beans as a List;
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    /**
     * Retrieve a paginated list of active Court Locations of type CHOA.
     *
     * <p>Supports optional filters on code and name, case-insensitive partial matches, and maps the
     * entity results into summary DTOs with page metadata.
     *
     * @param codeFilter optional partial filter on court location code
     * @param nameFilter optional partial filter on court name
     * @param pageable pagination and sorting configuration
     * @return a page of Court Location summaries
     */
    @Override
    @Transactional(readOnly = true)
    public CourtLocationPage getPage(String nameFilter, String codeFilter, PagingWrapper pageable) {
        return auditService.processAudit(
                CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT,
                unused -> {
                    final Page<NationalCourtHouse> dbPage =
                            repository.findAllActiveCourts(
                                    codeFilter, nameFilter, pageable.getPageable());

                    var responsePage = new CourtLocationPage();

                    if (responsePage.getContent() == null) {
                        responsePage.setContent(new ArrayList<>());
                    }

                    pageMapper.toPage(dbPage, responsePage, pageable.getSortStrings());

                    dbPage.forEach(
                            court -> responsePage.addContentItem(mapper.toSummaryDto(court)));

                    CodeAndName record = new CodeAndName(codeFilter, nameFilter);
                    AuditableResult<CourtLocationPage, NationalCourtHouse> result =
                            new AuditableResult<>(responsePage, mapper.toEntity(record));
                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
