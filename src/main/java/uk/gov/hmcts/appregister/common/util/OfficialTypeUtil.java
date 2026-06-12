package uk.gov.hmcts.appregister.common.util;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;

@Slf4j
@UtilityClass
@SuppressWarnings({"checkstyle:MemberName", "checkstyle:AbbreviationAsWordInName"})
public class OfficialTypeUtil {

    public final String MAGISTRATE_CODE = OfficialType.MAGISTRATE.getValue();
    public final String CLERK_CODE = OfficialType.CLERK.getValue();
    public final List<OfficialType> PRINTABLE_CODES =
            List.of(OfficialType.MAGISTRATE, OfficialType.CLERK);

    public OfficialType fromCode(String code) {
        if (code == null) {
            log.warn("Received null official type code. Defaulting to MAGISTRATE.");
            return OfficialType.MAGISTRATE;
        }

        try {
            return OfficialType.fromValue(code);
        } catch (IllegalArgumentException e) {
            log.warn("Received invalid official type code: {}. Defaulting to MAGISTRATE.", code);
            return OfficialType.MAGISTRATE;
        }
    }
}
