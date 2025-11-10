package uk.gov.hmcts.appregister.common.util;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

@Slf4j
@UtilityClass
public final class OfficialTypeUtil {

    public static final String MAGISTRATE_CODE = "M";
    public static final String CLERK_CODE = "C";
    public static final List<String> PRINTABLE_CODES = List.of(MAGISTRATE_CODE, CLERK_CODE);

    public static OfficialType fromCode(String code) {
        if (code == null) {
            log.warn("Received null official type code. Defaulting to MAGISTRATE.");
            return OfficialType.MAGISTRATE;
        }

        return switch (code) {
            case MAGISTRATE_CODE -> OfficialType.MAGISTRATE;
            case CLERK_CODE -> OfficialType.CLERK;
            default -> {
                log.warn("Invalid official type code: {}. Defaulting to MAGISTRATE.", code);
                yield OfficialType.MAGISTRATE;
            }
        };
    }
}
