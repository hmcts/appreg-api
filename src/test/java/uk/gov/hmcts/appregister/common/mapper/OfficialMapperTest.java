package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.appregister.common.enumeration.OfficialType.CLERK;
import static uk.gov.hmcts.appregister.common.enumeration.OfficialType.MAGISTRATE;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

class OfficialMapperTest {

    private final OfficialMapper mapper = new OfficialMapper();

    @Test
    void toOfficial_mapsBackendEnumToGeneratedEnum() {
        assertEquals(OfficialType.CLERK, mapper.toOfficial(CLERK));
        assertEquals(OfficialType.MAGISTRATE, mapper.toOfficial(MAGISTRATE));

        var officialType = (uk.gov.hmcts.appregister.common.enumeration.OfficialType) null;
        assertNull(mapper.toOfficial(officialType));
    }

    @Test
    void toOfficial_mapsGeneratedEnumToBackendEnum() {
        assertEquals(CLERK, mapper.toOfficial(OfficialType.CLERK));
        assertEquals(MAGISTRATE, mapper.toOfficial(OfficialType.MAGISTRATE));

        OfficialType officialType = null;
        assertNull(mapper.toOfficial(officialType));
    }
}
