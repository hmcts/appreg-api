package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

@Component
@Slf4j
@RequiredArgsConstructor
public class GetApplicationListEntriesValidator
        implements Validator<PayloadGetEntryInList, ApplicationList> {

    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(PayloadGetEntryInList validatable) {
        validate(validatable, null);
    }

    @Override
    public <R> R validate(
            PayloadGetEntryInList validatable,
            BiFunction<PayloadGetEntryInList, ApplicationList, R> validateSuccess) {
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuid(validatable.getListId());
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_NOT_FOUND,
                    "The application list with id %s was not found"
                            .formatted(validatable.getListId()));
        }

        if (validateSuccess != null) {
            return validateSuccess.apply(validatable, applicationList.get());
        }
        return null;
    }
}
