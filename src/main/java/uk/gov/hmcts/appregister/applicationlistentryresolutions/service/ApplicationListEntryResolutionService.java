package uk.gov.hmcts.appregister.applicationlistentryresolutions.service;

import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionWriteDto;

/** Service interface for managing application results. */
public interface ApplicationListEntryResolutionService {
    ApplicationListEntryResolutionDto getResultForApplication(Long listId, Long applicationId);

    ApplicationListEntryResolutionDto create(
            Long listId, Long applicationId, ApplicationListEntryResolutionWriteDto dto);

    ApplicationListEntryResolutionDto update(
            Long listId,
            Long applicationId,
            Long resultId,
            ApplicationListEntryResolutionWriteDto dto);

    void delete(Long listId, Long applicationId, Long resultId);
}
