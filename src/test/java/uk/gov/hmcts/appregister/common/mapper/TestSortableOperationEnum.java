package uk.gov.hmcts.appregister.common.mapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;

@Getter
@RequiredArgsConstructor
enum TestSortableOperationEnum implements SortableOperationEnum {
    TITLE("title", new String[] {ApplicationCode_.DESTINATION_EMAIL1}),
    CODE("code", new String[] {ApplicationCode_.REQUIRES_RESPONDENT});

    private final String apiValue;
    private final String[] entityValue;
}
