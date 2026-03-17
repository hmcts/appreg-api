package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.generated.model.Sort;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;

/**
 * A mapper that allows us to between Spring and our custom pagination format.
 */
@Component
public class PageMapper {
    /**
     * maps a spring page to our custom page object.
     *
     * @param from The spring page object
     * @param to The custom page object
     * @param originalSort The original sort values
     */
    public void toPage(
            org.springframework.data.domain.Page<?> from,
            uk.gov.hmcts.appregister.generated.model.Page to,
            List<SortableFieldMapper> originalSort) {
        to.setTotalElements(from.getTotalElements());
        to.setElementsOnPage(from.getNumberOfElements());
        to.setTotalPages(from.getTotalPages());
        to.setPageNumber(from.getPageable().getPageNumber());
        to.setPageSize(from.getPageable().getPageSize());
        to.setFirst(from.isFirst());
        to.setLast(from.isLast());
        to.setSort(toSort(originalSort));
    }

    /**
     * gets the domain specific sort object from a list of sort values.
     *
     * @param sortValues The spring sort object
     * @return The sort object
     */
    public Sort toSort(List<SortableFieldMapper> sortValues) {
        if (sortValues == null) {
            // Open API Schema allows null for sort.
            return null;
        }

        Sort s = new Sort();
        for (SortableFieldMapper sortValue : sortValues) {
            var item = new SortOrdersInner();
            item.setProperty(sortValue.getField());
            item.setDirection(
                    sortValue.isDirectionDescending()
                            ? SortOrdersInner.DirectionEnum.DESC
                            : SortOrdersInner.DirectionEnum.ASC);
            s.addOrdersItem(item);
        }
        return s;
    }
}
