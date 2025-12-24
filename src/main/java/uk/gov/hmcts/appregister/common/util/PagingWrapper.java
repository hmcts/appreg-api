package uk.gov.hmcts.appregister.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;

import uk.gov.hmcts.appregister.common.api.SortableField;

import java.util.List;

/**
 * A pageable wrapper class that holds the original Pageable details as well
 * as all of the original entry {@link SortableField}
 */
@RequiredArgsConstructor
@Getter
public class PageableWrapper {
    private final List<SortableField> sortStrings;
    private final Pageable pageable;

    /**
     * The original pageable details.
     * @param sort The sort details
     */
    public static PageableWrapper of(List<SortableField> sort, Pageable page) {
        return new PageableWrapper(sort, page);
    }
}
