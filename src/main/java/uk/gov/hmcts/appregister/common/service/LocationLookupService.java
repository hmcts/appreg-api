package uk.gov.hmcts.appregister.common.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;

/**
 * Centralised lookups for Courts and Criminal Justice Areas.
 *
 * <p>Guarantees a single match or throws a domain-specific {@link AppRegistryException}. Normalises
 * input (trimming) and provides Optional-based finders for callers that want to handle absence
 * themselves.
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

    /** Returns the single active court for the given code, or throws a domain exception. */
    public NationalCourtHouse getActiveCourtOrThrow(String code) {
        List<NationalCourtHouse> courts = courtHouseRepository.findActiveCourts(code);

        if (courts.isEmpty()) {
            throw new AppRegistryException(
                    CourtLocationError.COURT_NOT_FOUND,
                    "No court found for code '%s'".formatted(code));
        }
        if (courts.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    CourtLocationError.DUPLICATE_COURT_FOUND,
                    "Multiple courts found for code '%s'".formatted(code));
        }
        return courts.getFirst();
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
