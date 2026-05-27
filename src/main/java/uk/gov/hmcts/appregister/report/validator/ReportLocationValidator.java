package uk.gov.hmcts.appregister.report.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.exception.ReportError;

@Component
@RequiredArgsConstructor
public class ReportLocationValidator {
    private static final int SINGLE_RECORD = 1;

    private final CriminalJusticeAreaRepository criminalJusticeAreaRepository;
    private final NationalCourtHouseRepository courtHouseRepository;
    private final BusinessDateProvider businessDateProvider;

    public void validate(LegacyReportLocation location) {
        if (location == null) {
            return;
        }
        validateLocationCombination(location);
        validateCjaCode(location.getCjaCode());
        validateCourtLocationCode(location.getCourtLocationCode());
    }

    private void validateLocationCombination(LegacyReportLocation location) {
        boolean hasCourt = StringUtils.hasText(location.getCourtLocationCode());
        boolean hasCja = StringUtils.hasText(location.getCjaCode());
        boolean hasOtherLocation = StringUtils.hasText(location.getOtherLocationDescription());

        if (hasCourt && (hasCja || hasOtherLocation)) {
            throw new AppRegistryException(
                    ReportError.INVALID_LOCATION_COMBINATION,
                    "'courtLocationCode' cannot be combined with 'cjaCode' or "
                            + "'otherLocationDescription'.");
        }
    }

    private void validateCjaCode(String cjaCode) {
        if (!StringUtils.hasText(cjaCode)) {
            return;
        }

        var trimmedCjaCode = cjaCode.trim();
        var criminalJusticeAreas = criminalJusticeAreaRepository.findByCode(trimmedCjaCode);
        if (criminalJusticeAreas.isEmpty()) {
            throw new AppRegistryException(
                    ReportError.CJA_NOT_FOUND,
                    "No Criminal Justice Areas found for code '%s'".formatted(trimmedCjaCode));
        }
        if (criminalJusticeAreas.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    ReportError.DUPLICATE_CJA_FOUND,
                    "Multiple Criminal Justice Areas found for code '%s'"
                            .formatted(trimmedCjaCode));
        }
    }

    private void validateCourtLocationCode(String courtLocationCode) {
        if (!StringUtils.hasText(courtLocationCode)) {
            return;
        }

        var trimmedCourtLocationCode = courtLocationCode.trim();
        var todayUk = businessDateProvider.currentUkDate();
        if (courtHouseRepository.findActiveCourts(trimmedCourtLocationCode, todayUk).isEmpty()) {
            throw new AppRegistryException(
                    ReportError.COURT_NOT_FOUND,
                    "No court found for code '%s'".formatted(trimmedCourtLocationCode));
        }
    }
}
