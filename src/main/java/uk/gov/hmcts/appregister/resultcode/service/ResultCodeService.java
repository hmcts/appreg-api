package uk.gov.hmcts.appregister.resultcode.service;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;

/**
 * Service interface for Result Code operations.
 *
 * <p>Defines the business operations for retrieving Result Codes as required by the OpenAPI
 * contract and implemented by the controller layer.
 */
public interface ResultCodeService {

    /**
     * Find a specific Result Code by its code and effective date.
     *
     * <p>Searches for an active Result Code matches the provided {@code code} and is valid on the
     * given {@code date}. If no match is found or multiple matches exist, the implementation will
     * raise a domain-specific exception.
     *
     * @param code business identifier for the Result Code (case-insensitive)
     * @param date ISO date (yyyy-MM-dd) on which the Result Code must be valid
     * @return detailed Result Code DTO for the matching record
     */
    ResultCodeGetDetailDto findByCodeAndDate(String code, LocalDate date);

    /**
     * Retrieve a paginated list of active Result Codes.
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
     * @param codeFilter optional filter for Result Code
     * @param titleFilter optional filter for Result Code
     * @param pageable Spring Data paging and sorting configuration
     * @return a page of summarised Result Codes with metadata
     */
    ResultCodePage getPage(String codeFilter, String titleFilter, Pageable pageable);
}
