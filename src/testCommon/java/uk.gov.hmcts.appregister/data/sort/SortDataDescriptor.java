package uk.gov.hmcts.appregister.data.sort;

import lombok.Builder;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.List;
import java.util.function.Function;

@Builder
public class SortDataDescriptor {
    Function<Keyable, Object> sortableValueFunction;
    SortableOperationEnum sortableOperationEnum;
}
