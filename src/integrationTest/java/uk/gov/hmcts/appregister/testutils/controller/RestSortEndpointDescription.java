package uk.gov.hmcts.appregister.testutils.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

@Getter
@RequiredArgsConstructor
@Setter
public class RestSortEndpointDescription<T extends Keyable> {
    private SortMetaDescriptorEnum<T> sortDescriptors;

    private List<SortMetaDescriptorEnum<T>> allAvailableSortDescriptors;

    private List<T> expectedToBeGenerated = new ArrayList<>();

    /** The url to call. */
    private URL url;

    public void setExpectedToBeGenerated(List<T> expectedToBeGeneratedLst) {
        expectedToBeGenerated =
                new ArrayList<>(
                        expectedToBeGeneratedLst.stream().map(CopyUtil::deepClone).toList());
    }

    public List<SortMetaDescriptorEnum<T>> getAvailableSortDescriptorsExcludingActive() {
        return allAvailableSortDescriptors.stream().filter(des -> des != sortDescriptors).toList();
    }

    public RestSortEndpointDescription(RestSortEndpointDescription<T> description) {
        setUrl(description.getUrl());
        sortDescriptors = description.sortDescriptors;
    }

    @Override
    public String toString() {
        return "Sorting for "
                + sortDescriptors.getDescriptor().getSortableOperationEnum().getApiValue()
                + " "
                + sortDescriptors.getDescriptor().getOrder();
    }
}
