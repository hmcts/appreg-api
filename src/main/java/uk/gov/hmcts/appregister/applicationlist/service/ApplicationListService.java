package uk.gov.hmcts.appregister.applicationlist.service;

import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

import java.util.UUID;

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
     * @return a detailed DTO representing the newly created application list
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails,
     *     or the associated Court/CJA entity is not found or duplicated
     */
    ApplicationListGetDetailDto create(ApplicationListCreateDto dto);

    /**
     * Gets a new Application List.
     *
     * <p>
     * This method encapsulates all business logic required to:
     * <ul>
     *   <li>Fetch the list metadata and total entry count</li>
     *   <li>Query a lightweight projection of entry summaries ordered by sequence number</li>
     * </ul>
     * The operation is read-only and does not modify any data.
     * </p>
     *
     * @param id the unique identifier of the application list to retrieve
     * @return a detailed DTO representing the retrieved application list
     */
    ApplicationListGetDetailDto get(UUID id, Boolean includeSummaries);
}
