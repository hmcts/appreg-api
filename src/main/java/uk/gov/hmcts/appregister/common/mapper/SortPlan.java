package uk.gov.hmcts.appregister.common.mapper;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

/**
 * Captures the actual entity sorts to apply, plus an optional final tie-breaker.
 */
public record SortPlan(List<String> sortStrings, String tieBreaker) {
    public static SortPlan from(SortableOperationEnum sortableOperation, String direction) {
        List<String> sortParts = new ArrayList<>();
        for (String sort : sortableOperation.getEntityValue()) {
            if (direction != null) {
                sortParts.add(sort + "," + direction);
            } else {
                sortParts.add(sort);
            }
        }

        String tieBreaker = null;
        if (sortableOperation.getTieBreaker() != null) {
            tieBreaker = sortableOperation.getTieBreaker() + "," + direction;
        }

        return new SortPlan(sortParts, tieBreaker);
    }
}
