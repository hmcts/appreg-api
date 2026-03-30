package uk.gov.hmcts.appregister.data.filter;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class FilterScenarioFactory {

    /** The n umber of records being generated. */
    private static final int NUMBER_OF_RECORDS = 4;

    /**
     * gets the code using the full set of data descriptors. The scenario will
     * be generated with 2 records, one with the start values and one with the end values. These
     * can be used for sort ordering capabilities.
     *
     * @param keyable     The keyable for this scenario.
     * @param descriptors The descriptors to use.
     * @return The filterable data that has been genearted for the descriptors.
     */
    public static <T extends Keyable> FilterableScenario<T> createFilterScenario(T keyable,
                                                                                 List<FilterDescriptionEnum<T>> descriptors,
                                                                                 List<SortDescriptorEnum<T>> sortDescriptorEnums) {
        FilterableScenario<T> scenario = new FilterableScenario<T>();

        for (int i =0; i < NUMBER_OF_RECORDS; i++) {
            T copiedKey = (T) CopyUtil.deepClone(keyable);

            List<FilterFieldData<T>> filterFieldDataLst = new ArrayList<>();
            for (int j = 0; j < descriptors.size(); j++) {
                filterFieldDataLst.add(descriptors.get(j).getDescriptor().apply(i, copiedKey));
            }
            scenario.add(filterFieldDataLst);
        }

       scenario.setSortDescriptorEnums(sortDescriptorEnums);

        return scenario;
    }

    /**
     * creates a start and end keyable for sorting.
     * @param keyable The keyable to use for the sort.
     * @param sortDescriptorEnums The sort to apply
     */
    public static <T extends Keyable> List<T> createSort(T keyable,
        List<SortDescriptorEnum<T>> sortDescriptorEnums) {
        List<T> result = new ArrayList<>();
        for (int i =0; i < NUMBER_OF_RECORDS; i++) {
            result.add(CopyUtil.deepClone(keyable));
            applySort(i, result.getLast(), sortDescriptorEnums);
        }

        return result;
    }

    private static <T extends Keyable> void applySort(int count, T keyable, List<SortDescriptorEnum<T>> sortDescriptors) {
        for (SortDescriptorEnum<T> descriptorEnum : sortDescriptors) {
            descriptorEnum.getDescriptor().getSortGenerator().apply(count, keyable, descriptorEnum.getDescriptor());
        }
    }

}
