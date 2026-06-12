package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

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
    protected boolean isFeeStatusRequired(ApplicationCode applicationCode) {
        return false;
    }

    @Override
    protected CreateApplicationEntryValidationSuccess getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            FeePair fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            PayloadForCreate<EntryCreateDto> dto) {
        dto.getData()
                .setWordingFields(
                        trimAndKeyWordingFields(
                                code, wordingTemplateCollection, dto.getData().getWordingFields()));

        return super.getResult(code, wordingTemplateCollection, fee, saCode, applicationList, dto);
    }

    private static List<TemplateSubstitution> trimAndKeyWordingFields(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            List<TemplateSubstitution> suppliedWordingFields) {
        List<TemplateSubstitution> requiredWordingFields =
                wordingTemplateCollection.getKeysToBeSubstituted();
        List<TemplateSubstitution> supplied =
                suppliedWordingFields == null ? List.of() : suppliedWordingFields;

        if (requiredWordingFields.isEmpty()) {
            return List.of();
        }

        if (supplied.size() < requiredWordingFields.size()) {
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Not enough APPLICATION_TEXT values supplied for code %s"
                            .formatted(code.getCode()),
                    Map.of(
                            "templateSize",
                            Integer.toString(requiredWordingFields.size()),
                            "valueSize",
                            Integer.toString(supplied.size())));
        }

        return IntStream.range(0, requiredWordingFields.size())
                .mapToObj(
                        index -> {
                            TemplateSubstitution suppliedField = supplied.get(index);
                            String suppliedValue =
                                    suppliedField == null ? null : suppliedField.getValue();
                            if (StringUtils.isBlank(suppliedValue)) {
                                throw new AppRegistryException(
                                        CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                                        "APPLICATION_TEXT%s is required for code %s"
                                                .formatted(index + 1, code.getCode()),
                                        Map.of(
                                                "templateSize",
                                                Integer.toString(requiredWordingFields.size()),
                                                "valueSize",
                                                Integer.toString(supplied.size())));
                            }

                            return new TemplateSubstitution(
                                    requiredWordingFields.get(index).getKey(), suppliedValue);
                        })
                .toList();
    }
}
