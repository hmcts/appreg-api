package uk.gov.hmcts.appregister.common.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;

/**
 * Centralised lookups for Courts and Criminal Justice Areas.
 *
 * <p>Guarantees a deterministic match or throws a domain-specific {@link AppRegistryException}.
 * Where multiple active court rows exist, open-ended rows are already ordered first in the
 * repository and the first row is selected with a data-quality warning.
 *
 * <p>Place in: uk.gov.hmcts.appregister.location.service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationLookupService {

    private static final int SINGLE_RECORD = 1;

    private final NationalCourtHouseRepository courtHouseRepository;
    private final CriminalJusticeAreaRepository cjaRepository;
    private final BusinessDateProvider businessDateProvider;

    /** Returns the deterministically selected active court for the given code. */
    public NationalCourtHouse getActiveCourtOrThrow(String code) {
        LocalDate todayUk = businessDateProvider.currentUkDate();
        List<NationalCourtHouse> courts = courtHouseRepository.findActiveCourts(code, todayUk);

        if (courts.isEmpty()) {
            throw new AppRegistryException(
                    CourtLocationError.COURT_NOT_FOUND,
                    "No court found for code '%s'".formatted(code));
        }
        return ReferenceDataSelectionUtil.selectFirstOrderedActiveRecord(
                courts, "court location", code, todayUk, NationalCourtHouse::getEndDate);
    }

    /** Returns the single CJA for the given code, or throws a domain exception. */
    public CriminalJusticeArea getCjaOrThrow(String code) {
        List<CriminalJusticeArea> cjas = cjaRepository.findByCode(code);

        if (cjas.isEmpty()) {
            throw new AppRegistryException(
                    CriminalJusticeAreaError.CJA_NOT_FOUND,
                    "No Criminal Justice Areas found for code '%s'".formatted(code));
        }
        if (cjas.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    CriminalJusticeAreaError.DUPLICATE_CJA_FOUND,
                    "Multiple Criminal Justice Areas found for code '%s'".formatted(code));
        }
        return cjas.getFirst();
    }
}
