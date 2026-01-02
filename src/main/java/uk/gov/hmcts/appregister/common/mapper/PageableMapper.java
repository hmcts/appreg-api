package uk.gov.hmcts.appregister.common.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;

/**
 * A parser class that allows mapping of pageable parameters to the {@link
 * org.springframework.data.domain.Pageable}.
 */
@Component
@Getter
@Setter
public class PageableMapper {
    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    /**
     * map from a set of values to a spring pageable.
     *
     * @param page The page number (0 based)
     * @param size The page size
     * @param sort Each entry will contain a property and optionally a direction separated by a
     *     comma
     * @param defaultSortProperty The default property to sort on if no sort is specified
     * @param defaultDirection The default direction to sort if no sort is specified
     * @param findSortFieldEnum A mapper to the internal (entity) sortable field enum
     */
    public <T extends SortableOperationEnum> PagingWrapper from(
            Integer page,
            Integer size,
            List<String> sort,
            T defaultSortProperty,
            Sort.Direction defaultDirection,
            Function<String, T> findSortFieldEnum) {

        if (size != null && size > maxPageSize) {
            size = maxPageSize;
        }
        Sort sortSpec;

        List<SortableField> sortableFields = null;

        String tieBreaker = null;
        List<String> mappedSorts = new ArrayList<>();

        // process the sorts or default the sort
        if (sort != null && !sort.isEmpty()) {

            sortableFields = SortableField.of(sort.toArray(new String[0]));
            for (SortableField sortableField : sortableFields) {
                mappedSorts.addAll(
                        sortableField.toSortStringUsingSortableOperation(findSortFieldEnum));

                tieBreaker = sortableField.toTieBreaker(findSortFieldEnum);
            }
        } else {
            SortableField sortableField =
                    SortableField.of(
                                    defaultSortProperty.getApiValue()
                                            + ","
                                            + defaultDirection.name())
                            .getFirst();

            mappedSorts.addAll(sortableField.toSortStringUsingSortableOperation(findSortFieldEnum));

            tieBreaker = sortableField.toTieBreaker(findSortFieldEnum);
        }

        // if we have a tie breaker then add it to the end of the sort list
        if (tieBreaker != null) {
            mappedSorts.add(tieBreaker);
        }

        // apply disambiguation
        sortSpec = parseSort(mappedSorts);

        int p = (page == null || page < 0) ? 0 : page; // Spring pages are 0-based
        int s = (size == null || size < 1) ? defaultPageSize : size; // pick your default

        return PagingWrapper.of(sortableFields, PageRequest.of(p, s, sortSpec));
    }

    /**
     * parses the sort parameter from a list of strings.
     *
     * @param sort The list of sort parameters
     * @return The spring sort
     */
    public Sort parseSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (String raw : sort) {
            String token = raw != null ? raw.trim() : "";

            // set the default direction to be ascending
            Sort.Direction dir = Sort.Direction.ASC;
            String prop;

            // split the token by the comma to get the property and direction
            if (token.contains(",")) {
                String[] parts = token.split(",");
                prop = parts[0].trim();
                if (parts.length > 1) {
                    dir =
                            Sort.Direction.fromOptionalString(parts[1].trim())
                                    .orElse(Sort.Direction.ASC);
                }
            } else {
                prop = token;
            }

            // if we have a sort that is empty then error else parse
            if (!prop.isEmpty()) {
                orders.add(new Sort.Order(dir, prop).nullsLast());
            } else {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE, "Sort value %s is not suitable");
            }
        }

        return Sort.by(orders);
    }
}
