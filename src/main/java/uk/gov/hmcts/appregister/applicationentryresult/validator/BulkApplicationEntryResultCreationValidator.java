package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

@Component
@Slf4j
@RequiredArgsConstructor
public class BulkApplicationEntryResultCreationValidator
        implements Validator<
                PayloadForCreateResults<BulkResultDto>, BulkApplicationEntryResultCreationSuccess> {

    private final ApplicationListRepository applicationListRepository;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    @Override
    public void validate(PayloadForCreateResults<BulkResultDto> validatable) {
        validate(validatable, (v, r) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForCreateResults<BulkResultDto> validatable,
            BiFunction<
                            PayloadForCreateResults<BulkResultDto>,
                            BulkApplicationEntryResultCreationSuccess,
                            R>
                    validateSuccess) {

        Optional<ApplicationList> applicationListOptional;

        // lets cope with the situation where we may not have a list but if
        // we do the entries should relate to those entries
        if (validatable.getListId() != null) {

            log.debug("Validating bulk result entries for list {}", validatable.getListId());
            applicationListOptional = applicationListRepository.findByUuid(validatable.getListId());

            if (applicationListOptional.isEmpty()) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST,
                        "The list does not exist %s".formatted(validatable.getListId()));
            }

            // now validate the entries belong to the list
            boolean validated =
                    applicationListEntryRepository.doesApplicationEntryBelongToApplicationList(
                            validatable.getPayload().getEntryIds().stream().toList(),
                            validatable.getListId());

            // if this is not valid then error
            if (!validated) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError
                                .APPLICATION_ENTRY_RESULT_ENTRIES_NOT_IN_LIST,
                        "The bulk result entries are not in the same application list for %s"
                                .formatted(validatable.getListId()));
            }
        }

        BulkApplicationEntryResultCreationSuccess bulkSuccess =
                new BulkApplicationEntryResultCreationSuccess();

        // process the validator according to whether we have the list ids or not
        if (validatable.getListId() != null) {
            List<EntryToList> entryToListLst =
                    getEntryForList(
                            validatable.getListId(),
                            validatable.getPayload().getEntryIds().stream().toList());

            for (EntryToList entryToList : entryToListLst) {
                // validate the entries
                validateApplicationListEntryForResultCreation(
                        entryToList, validatable.getPayload(), bulkSuccess);
            }
        } else {
            // get all of the entry to list mappings
            List<EntryToList> entryToListMapping;
            entryToListMapping =
                    applicationListEntryRepository.findApplicationListForAllEntries(
                            validatable.getPayload().getEntryIds().stream().toList());

            if (entryToListMapping.size() < validatable.getPayload().getEntryIds().size()) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST,
                        "The entries are not all present");
            }

            // now process all of the list id entries
            for (EntryToList entryToList : entryToListMapping) {
                log.debug(
                        "Validating bulk result entries acquiring list as not present {}",
                        entryToList.listId());

                validateApplicationListEntryForResultCreation(
                        entryToList, validatable.getPayload(), bulkSuccess);
            }
        }

        // now pass the success details through to the callback so the logic can take place
        return validateSuccess.apply(validatable, bulkSuccess);
    }

    private List<EntryToList> getEntryForList(UUID listId, List<UUID> entryIds) {
        List<EntryToList> entryToLists = new ArrayList<>();

        for (UUID entryId : entryIds) {
            entryToLists.add(new EntryToList(entryId, listId));
        }

        return entryToLists;
    }

    /**
     * validates the application list entry for result creation.
     *
     * @param entryToList The entry to list mapping
     * @param bulkSuccess The success object to populate on each successful validation
     */
    private void validateApplicationListEntryForResultCreation(
            EntryToList entryToList,
            BulkResultDto resultDto,
            BulkApplicationEntryResultCreationSuccess bulkSuccess) {

        // now validate all associated details using the preexisting create validator
        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(resultDto.getResult().getResultCode());
        createDto.setWordingFields(resultDto.getResult().getWordingFields());

        // now execute the validation to ensure all resulted data is relevant
        PayloadForCreateEntryResult<ResultCreateDto> result =
                PayloadForCreateEntryResult.<ResultCreateDto>builder()
                        .listId(entryToList.listId())
                        .entryId(entryToList.entryId())
                        .data(createDto)
                        .build();

        bulkSuccess.getResults().add(result);
    }
}
