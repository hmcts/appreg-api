package uk.gov.hmcts.appregister.courtlocation.service;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

/**
 * Service interface for Court Location operations.
 *
 * <p>Defines the business operations for retrieving Court Locations as required by the OpenAPI
 * contract and implemented by the controller layer.
 */
public interface CourtLocationService {

    /**
     * Find a specific Court Location by its business code and effective date.
     *
     * <p>Searches for an active Court Location of type CHOA that matches the provided {@code code}
     * and is valid on the given {@code date}. If no match is found or multiple matches exist, the
     * implementation will raise a domain-specific exception.
     *
     * @param code business identifier for the Court Location (case-insensitive)
     * @param date ISO date (yyyy-MM-dd) on which the Court Location must be valid
     * @return detailed Court Location DTO for the matching record
     */
    CourtLocationGetDetailDto findByCodeAndDate(String code, LocalDate date);

    /**
     * Retrieve a paginated list of active CHOA Court Locations.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code name} — optional, case-insensitive partial match on court name
     *   <li>{@code code} — optional, case-insensitive partial match on court code
     * </ul>
     *
     * <p>Pagination and sorting are handled via the supplied {@link Pageable}, which is typically
     * created from OpenAPI paging parameters by a mapper.
     *
     * @param name optional filter for court name
     * @param code optional filter for court location code
     * @param pageable Spring Data paging and sorting configuration
     * @return a page of summarised Court Locations with metadata
     */
    CourtLocationPage getPage(String name, String code, PagingWrapper pageable);
}
