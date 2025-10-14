package uk.gov.hmcts.appregister.common.mapper;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

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
     */
    public org.springframework.data.domain.Pageable from(
            Integer page,
            Integer size,
            List<String> sort,
            String defaultSortProperty,
            Sort.Direction defaultDirection) {

        if (size != null && size > maxPageSize) {
            size = maxPageSize;
        }
        int p = (page == null || page < 0) ? 0 : page; // Spring pages are 0-based
        int s = (size == null || size < 1) ? defaultPageSize : size; // pick your default
        Sort sortSpec;

        // process the sorts or default the sort
        if (sort != null && !sort.isEmpty()) {
            sortSpec = parseSort(sort);
        } else {
            // default the sort
            sortSpec = Sort.by(defaultDirection, defaultSortProperty);
        }

        return PageRequest.of(p, s, sortSpec);
    }

    /**
     * parses the sort parameter from a list of strings.
     *
     * @param sort The list of sort parameters
     * @return The spring sort
     */
    private Sort parseSort(List<String> sort) {
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
                orders.add(new Sort.Order(dir, prop));
            } else {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE, "Sort value %s is not suitable");
            }
        }

        return Sort.by(orders);
    }
}
