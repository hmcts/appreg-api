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
     * @param sortConfig The sort policy containing the default internal sort and external lookup
     * @param defaultDirection The default direction to sort if no sort is specified
     */
    public PagingWrapper from(
            Integer page,
            Integer size,
            List<String> sort,
            SortConfig sortConfig,
            Sort.Direction defaultDirection) {
        return from(
                page,
                size,
                sort,
                sortConfig.defaultSort(),
                defaultDirection,
                sortConfig.externalLookup(),
                true);
    }

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
    public PagingWrapper from(
            Integer page,
            Integer size,
            List<String> sort,
            SortableOperationEnum defaultSortProperty,
            Sort.Direction defaultDirection,
            Function<String, ? extends SortableOperationEnum> findSortFieldEnum) {
        return from(
                page, size, sort, defaultSortProperty, defaultDirection, findSortFieldEnum, true);
    }

    private PagingWrapper from(
            Integer page,
            Integer size,
            List<String> sort,
            SortableOperationEnum defaultSortProperty,
            Sort.Direction defaultDirection,
            Function<String, ? extends SortableOperationEnum> findSortFieldEnum,
            boolean applyMaxPageSize) {

        validateAgainstMultipleSortSupported(sort);

        if (applyMaxPageSize && size != null && size > maxPageSize) {
            size = maxPageSize;
        }
        Sort sortSpec;

        List<SortableField> sortableFields = null;

        List<String> mappedSorts = new ArrayList<>();

        // process the sorts or default the sort
        if (sort != null && !sort.isEmpty()) {

            sortableFields = SortableField.of(sort.toArray(new String[0]));

            for (SortableField sortableField : sortableFields) {
                SortPlan sortPlan = sortableField.toSortPlan(findSortFieldEnum);
                mappedSorts.addAll(sortPlan.sortStrings());
                addTieBreaker(mappedSorts, sortPlan.tieBreaker());
            }
        } else {
            sortableFields = new ArrayList<>();
            SortableField sortableField =
                    SortableField.of(
                                    defaultSortProperty.getApiValue()
                                            + ","
                                            + defaultDirection.name())
                            .getFirst();

            SortPlan sortPlan = SortPlan.from(defaultSortProperty, sortableField.getDirection());
            mappedSorts.addAll(sortPlan.sortStrings());
            sortableFields.add(sortableField);
            addTieBreaker(mappedSorts, sortPlan.tieBreaker());
        }

        // apply disambiguation
        sortSpec = parseSort(mappedSorts);

        int p = (page == null || page < 0) ? 0 : page; // Spring pages are 0-based
        int s = (size == null || size < 1) ? defaultPageSize : size; // pick your default

        return PagingWrapper.of(sortableFields, PageRequest.of(p, s, sortSpec));
    }

    /**
     * Maps sort values to the first page of a pageable using the supplied limit as the page size.
     * Unlike request pageable overloads, this method does not cap the page size at the configured
     * maximum because bulk limits are validated separately.
     *
     * @param sort Each entry will contain a property and optionally a direction separated by a
     *     comma
     * @param limit The page size to request
     * @param sortConfig The sort policy containing the default internal sort and external lookup
     * @param defaultDirection The default direction to sort if no sort is specified
     * @return A paging wrapper for the first page using the supplied limit
     */
    public PagingWrapper from(
            List<String> sort, int limit, SortConfig sortConfig, Sort.Direction defaultDirection) {
        return from(
                0,
                limit,
                sort,
                sortConfig.defaultSort(),
                defaultDirection,
                sortConfig.externalLookup(),
                false);
    }

    private void addTieBreaker(List<String> mappedSorts, String tieBreaker) {
        if (tieBreaker != null) {
            mappedSorts.add(tieBreaker);
        }
    }

    /**
     * validates against multiple sort values. An exception is thrown if not.
     *
     * @param sort The sort values
     */
    private void validateAgainstMultipleSortSupported(List<String> sort) {
        // add a restriction based on application configuration
        if (sort != null && sort.size() > 1) {
            throw new AppRegistryException(
                    CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED,
                    "Multiple sort values are not allowed");
        }
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
            if (prop.isEmpty()) {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE, "Sort value %s is not suitable");
            }
            orders.add(new Sort.Order(dir, prop).nullsLast());
        }

        return Sort.by(orders);
    }
}
