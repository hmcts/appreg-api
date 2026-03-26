package uk.gov.hmcts.appregister.data.sort;

import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

public enum ApplicationCodeSort {
    public static SortDataDescriptor CODE = SortDataDescriptor
        .builder().sortableOperationEnum(ApplicationCodeSortFieldEnum.CODE)
        .sortableValueFunction(keyable -> ((ApplicationCode)keyable)
        .getCode()).build();

    public static SortDataDescriptor TITLE = SortDataDescriptor
        .builder().sortableOperationEnum(ApplicationCodeSortFieldEnum.TITLE)
        .sortableValueFunction(keyable -> ((ApplicationCode)keyable)
            .getTitle()).build();
}
