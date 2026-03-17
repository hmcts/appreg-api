package uk.gov.hmcts.appregister.common.util;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.mapper.SortableFieldMapper;

/**
 * A pageable wrapper class that holds the original Pageable details as well as the original sort
 * entries {@link SortableFieldMapper}. This is essential for returning the same sort details in our
 * page data.
 */
@RequiredArgsConstructor
@Getter
public class PagingWrapper {
    private final List<SortableFieldMapper> sortStrings;
    private final Pageable pageable;

    /**
     * The original pageable details.
     *
     * @param sort The sort details
     */
    public static PagingWrapper of(List<SortableFieldMapper> sort, Pageable page) {
        return new PagingWrapper(sort, page);
    }
}
