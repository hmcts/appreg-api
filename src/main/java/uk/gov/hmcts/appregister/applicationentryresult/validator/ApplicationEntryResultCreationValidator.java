package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

/**
 * Validates the dto for an application entry result create.
 */
@Component
@Slf4j
public class ApplicationEntryResultCreationValidator
        extends AbstractApplicationEntryResultValidator<
                PayloadForCreateEntryResult<ResultCreateDto>,
                ListEntryResultCreateValidationSuccess> {

    public ApplicationEntryResultCreationValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            ResolutionCodeRepository resolutionCodeRepository) {
        super(applicationListRepository, applicationListEntryRepository, resolutionCodeRepository);
    }

    @Override
    public void validate(PayloadForCreateEntryResult<ResultCreateDto> validatable) {
        validate(validatable, (v, s) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForCreateEntryResult<ResultCreateDto> validatable,
            BiFunction<
                            PayloadForCreateEntryResult<ResultCreateDto>,
                            ListEntryResultCreateValidationSuccess,
                            R>
                    validateSuccess) {

        return super.validate(validatable, validateSuccess);
    }

    @Override
    protected ListEntryResultCreateValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            PayloadForCreateEntryResult<ResultCreateDto> dto) {
        return ListEntryResultCreateValidationSuccess.builder()
                .applicationListEntry(applicationListEntry)
                .resolutionCode(code)
                .wordingSentence(wordingTemplateCollection)
                .build();
    }

    @Override
    protected String getResultCode(PayloadForCreateEntryResult<ResultCreateDto> validatable) {
        return validatable.getData().getResultCode();
    }

    @Override
    protected UUID getApplicationListUuid(
            PayloadForCreateEntryResult<ResultCreateDto> validatable) {
        return validatable.getListId();
    }

    @Override
    protected UUID getApplicationListEntryUuid(
            PayloadForCreateEntryResult<ResultCreateDto> validatable) {
        return validatable.getEntryId();
    }
}
