package uk.gov.hmcts.appregister.data.filter;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.util.CopyUtil;

@RequiredArgsConstructor
public class FilterScenarioFactory {
    /**
     * gets the code using the full set of data descriptors. The scenario will
     * be generated with 2 records, one with the start values and one with the end values. These
     * can be used for sort ordering capabilities.
     * @param keyable The keyable for this scenario.
     * @param descriptors The descriptors to use.
     * @return The filterable data that has been genearted for the descriptors.
     */
    public static FilterableScenario createFilterScenario(Keyable keyable,
                                                          FilterDescriptionEnum... descriptors) {
        FilterableScenario scenario = new FilterableScenario();

        // setup the start
        for (FilterDescriptionEnum enumDescriptor : descriptors) {
            scenario.getStartData().add(enumDescriptor.getDescriptor().apply(keyable, OrderEnum.START));
        }

        Keyable end = CopyUtil.deepClone(keyable);

        // setup the end
        for (FilterDescriptionEnum descriptor : descriptors) {
            scenario.getStartData().add(descriptor.getDescriptor().apply(end, OrderEnum.END));
        }

        return scenario;
    }
}
