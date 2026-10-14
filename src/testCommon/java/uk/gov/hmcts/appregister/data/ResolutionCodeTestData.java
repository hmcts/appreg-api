package uk.gov.hmcts.appregister.data;

import static org.instancio.Select.field;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.instancio.Instancio;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

public class ResolutionCodeTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ResolutionCode, ResolutionCode.ResolutionCodeBuilder> {
    private static final AtomicInteger NEXT_CODE = new AtomicInteger();

    @Override
    public ResolutionCode someComplete() {
        ResolutionCode code =
                Instancio.of(ResolutionCode.class)
                        .ignore(field(ResolutionCode::getId))
                        .ignore(field(ResolutionCode::getVersion))
                        .ignore(field(ResolutionCode::getResultCode))
                        .create();

        code.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        // Each test JVM has its own database; sequential codes avoid seed and sibling collisions.
        code.setResultCode("TR" + Integer.toUnsignedString(NEXT_CODE.incrementAndGet(), 36));
        return code;
    }
}
