package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.UUID;

import uk.gov.hmcts.appregister.generated.model.ApplicationListEntriesMovedDto;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

/**
 * Service interface for managing Application List Entries.
 *
 * <p>This service defines the contract for executing domain-level "actions" across
 * Application Lists and their entries.
 */
public interface ActionsService {

    /**
     * Moves the specified entries from a source Application List to a destination Application List.
     *
     * <p>This operation transfers one or more entries currently belonging to the source list identified
     * by {@code listId} to the destination list specified within the provided {@link MoveEntriesDto}.
     *
     * @param listId
     *     the identifier of the source Application List that currently owns the entries.
     * @param moveEntriesDto
     *     details of the destination list and the entries to be moved.
     * @return a DTO representing the outcome of the move operation.
     *
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails,
     *     or the associated target ApplicationList entity is not found
     */
    ApplicationListEntriesMovedDto move(UUID listId, MoveEntriesDto moveEntriesDto);
}
