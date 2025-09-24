package uk.gov.hmcts.appregister.common.mapper;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.generated.model.Sort;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;

/** A mapper that allows us to between Spring and our custom pagination format. */
@Component
public class PageMapper {
    /**
     * maps a spring page to our custom page object.
     *
     * @param from The spring page object
     * @param to The custom page object
     */
    public void toPage(
            org.springframework.data.domain.Page<?> from,
            uk.gov.hmcts.appregister.generated.model.Page to) {
        to.setTotalElements(from.getTotalElements());
        to.setElementsOnPage(from.getNumberOfElements());
        to.setTotalPages(from.getTotalPages());
        to.setPageNumber(from.getPageable().getPageNumber());
        to.setPageSize(from.getPageable().getPageSize());
        to.setFirst(from.isFirst());
        to.setLast(from.isLast());
        to.setSort(toSort(from.getSort()));
    }

    /**
     * gets the sort object from spring and maps it to our custom sort object.
     *
     * @param from The spring sort object
     * @return The sort object
     */
    private Sort toSort(org.springframework.data.domain.Sort from) {

        if (from == null || from.isUnsorted()) {
            // Open API Schema allows null for sort.
            return null;
        }

        var s = new Sort();
        for (org.springframework.data.domain.Sort.Order o : from) {
            var item = new SortOrdersInner();
            item.setProperty(o.getProperty());
            item.setDirection(
                o.getDirection() == org.springframework.data.domain.Sort.Direction.ASC
                    ? SortOrdersInner.DirectionEnum.ASC
                    : SortOrdersInner.DirectionEnum.DESC
            );
            s.addOrdersItem(item);
        }
        return s;
    }
}
