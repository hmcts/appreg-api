package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;

class AppConfigTest {

    private final AppConfig.ListStringConverter converter =
            new AppConfig().new ListStringConverter();

    @Test
    void convert_whenSourceIsString_thenReturnsSingleValueList() {
        Object converted =
                converter.convert(
                        "sortField",
                        TypeDescriptor.valueOf(String.class),
                        TypeDescriptor.valueOf(List.class));

        assertThat(converted).isEqualTo(List.of("sortField"));
    }

    @Test
    void convert_whenSourceIsNotString_thenReturnsNull() {
        Object converted =
                converter.convert(
                        1,
                        TypeDescriptor.valueOf(Integer.class),
                        TypeDescriptor.valueOf(List.class));

        assertThat(converted).isNull();
    }
}
