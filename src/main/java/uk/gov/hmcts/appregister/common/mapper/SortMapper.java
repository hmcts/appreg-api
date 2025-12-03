package uk.gov.hmcts.appregister.common.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

/**
 * Maps and validates API sort parameters for Application List queries.
 *
 * <p>Acts as the bridge between external API field names and internal entity properties, enforcing
 * allowed fields and sort directions. Delegates field validation to {@link
 * uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListSortValidator}.
 */
@Component
@RequiredArgsConstructor
public class SortMapper {
    /**
     * Translates API sort fields (e.g. "date,desc") into validated entity property names used for
     * persistence queries. If the client provides no sort fields, an empty list is returned so that
     * the caller can apply default sorting behavior.
     *
     * @param sorts e.g. ["date,desc","status,asc"]
     * @return e.g. ["listDate,desc","status,asc"]
     */
    public List<String> map(
            List<SortableField> sorts, Function<String, SortableOperationEnum> findSortFieldEnum) {
        List<String> mappedSorts = new ArrayList<>();
        for (SortableField sortableField : sorts) {
            mappedSorts.addAll(sortableField.toSortStringUsingSortableOperation(findSortFieldEnum));
        }
        return mappedSorts;
    }
}
