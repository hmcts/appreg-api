package uk.gov.hmcts.appregister.data.filter;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class FilterScenarioFactory {
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
                                                                                 List<FilterDescriptionEnum<T>> descriptors) {
        FilterableScenario<T> scenario = new FilterableScenario<T>();

        // setup the start
        for (FilterDescriptionEnum enumDescriptor : descriptors) {
            scenario.getStartData().add(enumDescriptor.getDescriptor().apply(keyable, OrderEnum.START));
        }

        T end = (T) CopyUtil.deepClone(keyable);

        // setup the end
        for (FilterDescriptionEnum descriptor : descriptors) {
            scenario.getEndData().add(descriptor.getDescriptor().apply(end, OrderEnum.END));
        }

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
         applySort(CopyUtil.deepClone(keyable), sortDescriptorEnums, OrderEnum.START);
         applySort(CopyUtil.deepClone(keyable), sortDescriptorEnums, OrderEnum.END);
         return result;
    }

    private static <T extends Keyable> void applySort(T keyable, List<SortDescriptorEnum<T>> sortDescriptors, OrderEnum orderEnum) {
        for (SortDescriptorEnum<T> descriptorEnum : sortDescriptors) {
            descriptorEnum.getDescriptor().getSortGenerator().apply(keyable, descriptorEnum.getDescriptor(), orderEnum);
        }
    }

}
