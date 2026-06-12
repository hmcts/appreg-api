package uk.gov.hmcts.appregister.standardapplicant.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;

@ExtendWith(MockitoExtension.class)
class StandardApplicantExistsValidatorTest {
    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
    private static final LocalDate TODAY_UK = LocalDate.of(2026, Month.JUNE, 9);

    @Mock private StandardApplicantRepository standardApplicantRepository;

    private StandardApplicantExistsValidator validator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-09T10:00:00Z"), ZoneId.of("UTC"));
        validator =
                new StandardApplicantExistsValidator(standardApplicantRepository, clock, UK_ZONE);
    }

    @Test
    void successValidation() {
        String code = "test";
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCode(code, TODAY_UK))
                .thenReturn(List.of(standardApplicant));

        // call the validator. No assertions needed as no exception means success
        validator.validate(code);
    }

    @Test
    void successValidationCallback() {
        String code = "test";
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCode(code, TODAY_UK))
                .thenReturn(List.of(standardApplicant));

        BiFunction<String, StandardApplicant, StandardApplicantGetDetailDto> biFunction =
                mockCallback();

        // call the validator. No assertions needed as no exception means success
        validator.validate(code, biFunction);

        Mockito.verify(biFunction).apply(code, standardApplicant);
    }

    @Test
    void successValidationFailNoCallback() {
        String code = "test";
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCode(code, TODAY_UK))
                .thenReturn(List.of());

        BiFunction<String, StandardApplicant, StandardApplicantGetDetailDto> biFunction =
                mockCallback();

        // call the validator. An exception is thrown but no callback is made to signify success
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(code, biFunction));
        Assertions.assertNotNull(appRegistryException);
        Mockito.verify(biFunction, Mockito.never()).apply(code, standardApplicant);
    }

    @Test
    void successValidationFailureNotFound() {
        String code = "test";
        when(standardApplicantRepository.findStandardApplicantByCode(code, TODAY_UK))
                .thenReturn(List.of());

        // call the validator. No assertions needed as no exception means success
        AppRegistryException appRegistryException =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(code));
        Assertions.assertEquals(
                StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getAppCode(),
                appRegistryException.getCode().getCode().getAppCode());
        Assertions.assertEquals(
                StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND
                        .getCode()
                        .getHttpCode()
                        .value(),
                appRegistryException.getCode().getCode().getHttpCode().value());
    }

    @Test
    void successValidationFailureDuplicate_prefersFirstRecord() {
        String code = "test";
        StandardApplicant standardApplicant = new StandardApplicant();
        StandardApplicant alternativeApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCode(code, TODAY_UK))
                .thenReturn(List.of(standardApplicant, alternativeApplicant));

        LogCaptor logCaptor = LogCaptor.forClass(ReferenceDataSelectionUtil.class);

        StandardApplicant actual = validator.validate(code, (request, applicant) -> applicant);

        Assertions.assertSame(standardApplicant, actual);
        assertThat(logCaptor.getWarnLogs().getFirst()).contains("Data quality warning");
    }

    @SuppressWarnings("unchecked")
    private static BiFunction<String, StandardApplicant, StandardApplicantGetDetailDto>
            mockCallback() {
        return mock(BiFunction.class);
    }
}
