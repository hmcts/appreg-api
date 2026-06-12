package uk.gov.hmcts.appregister.generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;
import utils.ConstraintAssertion;

class ApplicationEntryResultDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testEntryResultCreateDtoEmptyStringErrors() {
        // Create an instance of EntryCreateDto
        ResultCreateDto resultCreateDto = new ResultCreateDto();

        // Set properties
        resultCreateDto.setResultCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) resultCreateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "resultCode", "size must be between 1 and 10");
    }

    @Test
    void testEntryResultUpdateDtoEmptyStringErrors() {
        ResultUpdateDto resultUpdateDto = new ResultUpdateDto();

        // Set properties
        resultUpdateDto.setResultCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) resultUpdateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "resultCode", "size must be between 1 and 10");
    }

    @Test
    void testBulkResultDtoPreservesDuplicateEntryIds() throws Exception {
        UUID entryId = UUID.randomUUID();
        String payload =
                """
                {
                  "entryIds": [
                    "%s",
                    "%s"
                  ],
                  "result": {
                    "resultCode": "RTC"
                  }
                }
                """
                        .formatted(entryId, entryId);

        BulkResultDto bulkResultDto = objectMapper.readValue(payload, BulkResultDto.class);

        Assertions.assertEquals(List.of(entryId, entryId), bulkResultDto.getEntryIds());
    }
}
