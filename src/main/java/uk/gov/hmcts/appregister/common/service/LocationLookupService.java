package uk.gov.hmcts.appregister.common.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;

/**
 * Centralised lookups for Criminal Justice Areas.
 */
@Service
@RequiredArgsConstructor
public class LocationLookupService {

    private static final int SINGLE_RECORD = 1;

    private final CriminalJusticeAreaRepository cjaRepository;

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
