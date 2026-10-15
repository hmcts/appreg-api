package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.UUID;
import org.instancio.Instancio;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

public class ResolutionCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ResolutionCode, ResolutionCode.ResolutionCodeBuilder> {

    @Override
    public ResolutionCode someComplete() {
        ResolutionCode code =
                Instancio.of(ResolutionCode.class)
                        .ignore(field(ResolutionCode::getId))
                        .ignore(field(ResolutionCode::getVersion))
                        .create();

        code.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        return code;
    }
}
