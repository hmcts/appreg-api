package uk.gov.hmcts.appregister.common.async.reader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A language agnostic way of paging data.
 */
@AllArgsConstructor
@Getter
@Setter
public class ReadPagePosition {
    /** The page size to read. */
    int pageSize = 10;

    /** The offset to start reading from. */
    int startOffset;
}
