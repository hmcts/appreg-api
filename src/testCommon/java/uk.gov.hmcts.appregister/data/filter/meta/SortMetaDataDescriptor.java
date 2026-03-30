package uk.gov.hmcts.appregister.data.filter.meta;

import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.mapper.SortableField;

@Builder
@Setter
@Getter
public class SortMetaDataDescriptor<T> {
    /** Is the sort meta data default. */
    @Builder.Default boolean defaultSort = false;

    /** The order of the sort. */
    @Builder.Default String order = SortableField.ASC;

    /** The enum to determine the api value used in production code. */
    SortableOperationEnum sortableOperationEnum;

    /** The generator to use to generate the sort value. */
    GenerateAccordingToSort<T> sortGenerator;

    /** Returns the function that allows us to get a value to sort. */
    Function<T, Object> sortableValueFunction;
}
