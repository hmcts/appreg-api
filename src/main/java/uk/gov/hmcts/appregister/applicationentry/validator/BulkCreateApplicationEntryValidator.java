package uk.gov.hmcts.appregister.applicationentry.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

/**
 * Validates application entries created from bulk upload rows.
 */
@Component
public class BulkCreateApplicationEntryValidator extends CreateApplicationEntryValidator {

    public BulkCreateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            ApplicationFeeService feeService,
            BusinessDateProvider businessDateProvider,
            StandardApplicantRepository standardApplicantRepository) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeService,
                businessDateProvider,
                standardApplicantRepository);
    }

    @Override
    protected boolean isFeeStatusRequired(
            ApplicationCode applicationCode, PayloadForCreate<EntryCreateDto> validatable) {
        return false;
    }
}
