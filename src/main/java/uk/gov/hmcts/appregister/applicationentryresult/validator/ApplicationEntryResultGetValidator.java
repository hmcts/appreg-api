package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

@Component
@Slf4j
public class ApplicationEntryResultGetValidator
        extends AbstractApplicationEntryResultValidator<
                PayloadGetEntryResultInList, ListEntryResultGetValidationSuccess> {

    public ApplicationEntryResultGetValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            ResolutionCodeRepository resolutionCodeRepository,
            BusinessDateProvider businessDateProvider) {
        super(
                applicationListRepository,
                applicationListEntryRepository,
                resolutionCodeRepository,
                businessDateProvider);
    }

    @Override
    protected ListEntryResultGetValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            PayloadGetEntryResultInList dto) {
        return ListEntryResultGetValidationSuccess.builder()
                .applicationListEntry(applicationListEntry)
                .applicationList(applicationList)
                .build();
    }

    @Override
    public void validate(PayloadGetEntryResultInList args) {
        validate(args, (a, s) -> null);
    }

    @Override
    public <R> R validate(
            PayloadGetEntryResultInList args,
            BiFunction<PayloadGetEntryResultInList, ListEntryResultGetValidationSuccess, R>
                    createSupplier) {

        return super.validate(args, createSupplier);
    }

    @Override
    protected String getResultCode(PayloadGetEntryResultInList validatable) {
        return null;
    }

    @Override
    protected UUID getApplicationListUuid(PayloadGetEntryResultInList validatable) {
        return validatable.getListId();
    }

    @Override
    protected UUID getApplicationListEntryUuid(PayloadGetEntryResultInList validatable) {
        return validatable.getEntryId();
    }

    @Override
    protected void validateParentApplicationListIsOpen(ApplicationList validatable) {
        // Do not fail if the list is closed
    }
}
