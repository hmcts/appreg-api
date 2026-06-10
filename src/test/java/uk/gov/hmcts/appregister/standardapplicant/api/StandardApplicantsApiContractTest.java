package uk.gov.hmcts.appregister.standardapplicant.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.api.StandardApplicantsApi;

class StandardApplicantsApiContractTest {
    private static final Path STANDARD_APPLICANT_BY_CODE_OPENAPI_PATH =
            Path.of(
                    "src/main/resources/openapi/paths/standard-applicants/"
                            + "standard-applicants-by-code.yaml");

    @Test
    void getStandardApplicantByCodeOpenApiPathDoesNotDefineDateParameter() throws IOException {
        String pathYaml = Files.readString(STANDARD_APPLICANT_BY_CODE_OPENAPI_PATH);

        assertThat(pathYaml).contains("operationId: getStandardApplicantByCode");
        assertThat(pathYaml).doesNotContain("name: date");
        assertThat(pathYaml).doesNotContain("getStandardApplicantByCodeAndDate");
    }

    @Test
    void generatedStandardApplicantsApiHasCodeOnlyLookupMethod() {
        Method[] methods = StandardApplicantsApi.class.getMethods();

        Assertions.assertTrue(
                Arrays.stream(methods)
                        .anyMatch(
                                method ->
                                        method.getName().equals("getStandardApplicantByCode")
                                                && method.getParameterCount() == 1
                                                && method.getParameterTypes()[0].equals(
                                                        String.class)));
        Assertions.assertFalse(
                Arrays.stream(methods)
                        .anyMatch(
                                method ->
                                        method.getName()
                                                .equals("getStandardApplicantByCodeAndDate")));
    }
}
