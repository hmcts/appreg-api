package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

/**
 * Service interface for managing Application Lists.
 *
 * <p>This service defines the contract for creating and retrieving application lists within the
 * registry system. Implementations must ensure validation, persistence, and appropriate mapping of
 * domain entities to DTOs.
 */
public interface ApplicationListService {

    /**
     * Creates a new Application List.
     *
     * <p>The input DTO is validated and then persisted. Depending on the presence of a Court
     * Location Code or a Criminal Justice Area (CJA) Code, the Application List is associated with
     * either a {@code NationalCourtHouse} or a {@code CriminalJusticeArea}.
     *
     * @param dto the data transfer object containing details for the application list to create
     * @return a detailed DTO representing the newly created application list. Each DTO is wrapped
     *     in a {@link uk.gov.hmcts.appregister.common.concurrency.MatchResponse} that can be used
     *     in update or delete operations match response which contains the etag information that
     *     can then be used for concurrency control in
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails,
     *     or the associated Court/CJA entity is not found or duplicated
     */
    MatchResponse<ApplicationListGetDetailDto> create(ApplicationListCreateDto dto);

    /**
     * Updates a new Application List.
     *
     * <p>The input DTO is validated and then persisted. Depending on the presence of a Court
     * Location Code or a Criminal Justice Area (CJA) Code, the Application List is associated with
     * either a {@code NationalCourtHouse} or a {@code CriminalJusticeArea}.
     *
     * @param dto the data transfer object containing details for the application list to update
     * @return a detailed DTO representing the newly created application list. Each DTO is wrapped
     *     in a {@link uk.gov.hmcts.appregister.common.concurrency.MatchResponse} that canontains an
     *     etag that can be used in update or delete operations
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails,
     *     or the associated Court/CJA entity is not found or duplicated
     */
    MatchResponse<ApplicationListGetDetailDto> update(
            PayloadForUpdate<ApplicationListUpdateDto> dto);

    /**
     * Gets a new Application List.
     *
     * <p>This method encapsulates all business logic required to:
     *
     * <ul>
     *   <li>Fetch the list metadata and total entry count
     *   <li>Query a lightweight projection of entry summaries ordered by sequence number
     * </ul>
     *
     * <p>The operation is read-only and does not modify any data. Pagination and sorting are
     * handled via the supplied {@link Pageable}, which is typically created from OpenAPI paging
     * parameters by a mapper.
     *
     * @param id the unique identifier of the application list to retrieve
     * @param pageable Spring Data paging and sorting configuration for entry summaries
     * @return a detailed DTO representing the retrieved application list
     */
    ApplicationListGetDetailDto get(UUID id, Pageable pageable);

    /**
     * Deletes an Application List.
     *
     * @param idToDelete the id to delete
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails
     *     due to the id not existing, the id already being deleted, the id having application
     *     entries
     */
    void delete(UUID idToDelete);

    /**
     * Retrieves a paginated collection of Application Lists matching the specified filter criteria.
     *
     * <p>This operation supports filtering by status, court location code, Criminal Justice Area
     * (CJA) code, date, time, and descriptive fields. Pagination and sorting parameters are applied
     * according to the provided {@link Pageable}.
     *
     * <p>The returned {@link ApplicationListPage} includes pagination metadata (page number, total
     * elements, total pages) and a list of {@link
     * uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto} items representing
     * summarized application list entries.
     *
     * @param dto the filter parameters used to constrain the search results
     * @param pageable pagination and sorting configuration
     * @return an {@link ApplicationListPage} containing a paginated set of application list
     *     summaries
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if invalid filter
     *     parameters are provided or underlying data retrieval fails
     */
    ApplicationListPage getPage(ApplicationListGetFilterDto dto, Pageable pageable);

    /**
     * Gets an Application List with all its Application List Entries.
     *
     * <p>This method encapsulates all business logic required to:
     *
     * <ul>
     *   <li>Fetch the list metadata and its entries
     *   <li>Query entries ordered by sequence number
     * </ul>
     *
     * <p>The operation is read-only and does not modify any data.
     *
     * @param id the unique identifier of the application list to retrieve
     * @return a detailed DTO representing the retrieved application list
     */
    ApplicationListGetPrintDto print(UUID id);
}
