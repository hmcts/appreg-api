package uk.gov.hmcts.appregister.testutils.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Setter
public class RestSortEndpointDescription<T extends Keyable> {
    private SortDescriptorEnum<T> sortDescriptors;

    private List<SortDescriptorEnum<T>> allAvailableSortDescriptors;

    private List<T> expectedToBeGenerated = new ArrayList<>();

    /** The url to call. */
    private URL url;

    public void setExpectedToBeGenerated(List<T> expectedToBeGeneratedLst) {
        expectedToBeGenerated = new ArrayList<>(expectedToBeGeneratedLst.stream()
            .map(CopyUtil::deepClone).toList());
    }

    public List<SortDescriptorEnum<T>> getAvailableSortDescriptorsExcludingActive() {
        return allAvailableSortDescriptors.stream().filter(des -> des
            != sortDescriptors).toList();
    }

    public RestSortEndpointDescription(RestSortEndpointDescription<T> description) {
        setUrl(description.getUrl());
        sortDescriptors = description.sortDescriptors;
    }

    @Override
    public String toString() {
        return "Sorting for " + sortDescriptors.getDescriptor().getSortableOperationEnum().getApiValue()
            + " " + sortDescriptors.getDescriptor().getOrder();
    }
}
