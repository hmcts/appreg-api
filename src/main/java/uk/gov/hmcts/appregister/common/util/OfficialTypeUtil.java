package uk.gov.hmcts.appregister.common.util;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;

@Slf4j
@UtilityClass
public final class OfficialTypeUtil {

    public static final String MAGISTRATE_CODE = OfficialType.MAGISTRATE.getValue();
    public static final String CLERK_CODE = OfficialType.CLERK.getValue();
    public static final List<OfficialType> PRINTABLE_CODES =
            List.of(OfficialType.MAGISTRATE, OfficialType.CLERK);

    public static OfficialType fromCode(String code) {
        if (code == null) {
            log.warn("Received null official type code. Defaulting to MAGISTRATE.");
            return OfficialType.MAGISTRATE;
        }

        try {
            OfficialType type = OfficialType.fromValue(code);
            return type;
        } catch (IllegalArgumentException e) {
            log.warn("Received invalid official type code: {}. Defaulting to MAGISTRATE.", code);
            return OfficialType.MAGISTRATE;
        }
    }
}
