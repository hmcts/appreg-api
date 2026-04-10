package uk.gov.hmcts.appregister.common.util;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.mapper.SortableField;

/**
 * A pageable wrapper class that holds the original Pageable details as well as the original sort
 * entries {@link SortableField}. This is essential for returning the same sort details in our page
 * data.
 */
@RequiredArgsConstructor
@Getter
public class PagingWrapper {
    private final List<SortableField> sortStrings;
    private final Pageable pageable;
    private final PagingSortMode sortMode;

    public PagingWrapper(List<SortableField> sortStrings, Pageable pageable) {
        this(sortStrings, pageable, PagingSortMode.REQUESTED);
    }

    /**
     * Creates a {@link PagingWrapper} using the provided sort and pageable information.
     *
     * @param sort the parsed API sort fields
     * @param page the resolved {@link Pageable}
     * @return a {@link PagingWrapper} with {@code REQUESTED} sort mode
     */
    public static PagingWrapper of(List<SortableField> sort, Pageable page) {
        return new PagingWrapper(sort, page, PagingSortMode.REQUESTED);
    }

    /**
     * Creates a {@link PagingWrapper} with an explicit {@link PagingSortMode}.
     *
     * @param sort the parsed API sort fields
     * @param page the resolved {@link Pageable}
     * @param sortMode the explicit sort mode to apply
     * @return a {@link PagingWrapper} with the given sort mode
     */
    public static PagingWrapper of(
            List<SortableField> sort, Pageable page, PagingSortMode sortMode) {
        return new PagingWrapper(sort, page, sortMode);
    }
}
