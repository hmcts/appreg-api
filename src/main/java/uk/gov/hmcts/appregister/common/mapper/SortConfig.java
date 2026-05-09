package uk.gov.hmcts.appregister.common.mapper;

import java.util.function.Function;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;

/**
 * Holds the sort rules for an endpoint: the fallback sort and the allowed request sort lookup.
 */
public record SortConfig(
        SortableOperationEnum defaultSort,
        Function<String, ? extends SortableOperationEnum> externalLookup) {}
