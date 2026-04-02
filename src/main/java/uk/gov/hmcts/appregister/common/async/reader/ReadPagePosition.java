package uk.gov.hmcts.appregister.common.async.reader;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class ReadPagePosition {

    int offset;

    @Setter(AccessLevel.NONE)
    final int limit;
}
