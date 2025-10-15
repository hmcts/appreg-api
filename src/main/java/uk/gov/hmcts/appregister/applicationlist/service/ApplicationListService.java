package uk.gov.hmcts.appregister.applicationlist.service;

import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
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
}
