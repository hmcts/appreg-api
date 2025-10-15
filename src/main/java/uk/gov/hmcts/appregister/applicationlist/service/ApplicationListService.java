package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.UUID;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

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
     * Deletes an Application List.
     *
     * @param idToDelete the id to delete
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails
     *     due to the id not existing, the id already being deleted, the id having application
     *     entries
     */
    void delete(UUID idToDelete);
}
