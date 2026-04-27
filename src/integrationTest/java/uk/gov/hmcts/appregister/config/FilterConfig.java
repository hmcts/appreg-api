package uk.gov.hmcts.appregister.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.appregister.filter.AllFilterCombinationScenarioStrategy;
import uk.gov.hmcts.appregister.filter.EssentialFilterScenarioStrategy;

/**
 * A filter scenario strategy that allows us the ability to apply different scenarios based on
 * environment variables or configuration.
 */
@Configuration
public class FilterConfig {
    @Bean
    @ConditionalOnProperty(name = "all.filter.enabled", havingValue = "true")
    public AllFilterCombinationScenarioStrategy allFilterCombinationScenarioStrategy() {
        return new AllFilterCombinationScenarioStrategy();
    }

    @Bean
    @ConditionalOnProperty(
            name = "all.filter.enabled",
            havingValue = "false",
            matchIfMissing = true)
    public EssentialFilterScenarioStrategy essentialFilterScenarioStrategy() {
        return new EssentialFilterScenarioStrategy();
    }
}
