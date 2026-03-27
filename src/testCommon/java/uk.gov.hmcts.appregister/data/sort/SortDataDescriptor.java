package uk.gov.hmcts.appregister.data.sort;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToSort;

import java.util.function.Function;

@Builder
@Setter
@Getter
public class SortDataDescriptor<T> {
    boolean defaultSort = false;
    @Builder.Default
    String order = SortableField.ASC;
    Function<T, Object> sortableValueFunction;
    SortableOperationEnum sortableOperationEnum;
    GenerateAccordingToSort<T> sortGenerator;
}
