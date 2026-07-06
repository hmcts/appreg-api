package uk.gov.hmcts.appregister.csds.ingress;

import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CsdsIngressProperties.class)
public class CsdsIngressConfiguration {
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder csdsIngressRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "appreg.csds.ingress", name = "enabled", havingValue = "true")
    RestClient csdsIngressRestClient(
            RestClient.Builder restClientBuilder, CsdsIngressProperties properties) {
        val requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                Math.toIntExact(properties.getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getReadTimeout().toMillis()));

        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
