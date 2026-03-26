package uk.gov.hmcts.appregister.standardapplicant.validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;

@ExtendWith(MockitoExtension.class)
public class StandardApplicantExistsValidatorTest {

    @Mock private StandardApplicantExistsValidator standardApplicantExistsValidator;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    @InjectMocks private StandardApplicantExistsValidator validator;

    @Test
    public void successValidation() {
        LocalDate localDate = LocalDate.now();
        String code = "test";
        PayloadForGet payload = PayloadForGet.builder().date(LocalDate.now()).code(code).build();
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(code, localDate))
                .thenReturn(List.of(standardApplicant));

        // call the validator. No assertions needed as no exception means success
        validator.validate(payload);
    }

    @Test
    public void successValidationCallback() {
        LocalDate localDate = LocalDate.now();
        String code = "test";
        PayloadForGet payload = PayloadForGet.builder().date(LocalDate.now()).code(code).build();
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(code, localDate))
                .thenReturn(List.of(standardApplicant));

        BiFunction<PayloadForGet, StandardApplicant, StandardApplicantGetDetailDto> biFunction =
                mock(BiFunction.class);

        // call the validator. No assertions needed as no exception means success
        validator.validate(payload, biFunction);

        Mockito.verify(biFunction, times(1)).apply(payload, standardApplicant);
    }

    @Test
    public void successValidationFailNoCallback() {
        LocalDate localDate = LocalDate.now();
        String code = "test";
        PayloadForGet payload = PayloadForGet.builder().date(LocalDate.now()).code(code).build();
        StandardApplicant standardApplicant = new StandardApplicant();
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(code, localDate))
                .thenReturn(List.of());

        BiFunction<PayloadForGet, StandardApplicant, StandardApplicantGetDetailDto> biFunction =
                mock(BiFunction.class);

        // call the validator. An exception is thrown but no callback is made to signify success
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload, biFunction));
        Assertions.assertNotNull(appRegistryException);
        Mockito.verify(biFunction, times(0)).apply(payload, standardApplicant);
    }

    @Test
    public void successValidationFailureNotFound() {
        LocalDate localDate = LocalDate.now();
        String code = "test";
        PayloadForGet payload = PayloadForGet.builder().date(LocalDate.now()).code(code).build();
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(code, localDate))
                .thenReturn(List.of());

        // call the validator. No assertions needed as no exception means success
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));
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
    public void successValidationFailureDuplicate_prefersFirstRecord() {
        LocalDate localDate = LocalDate.now();
        String code = "test";
        StandardApplicant standardApplicant = new StandardApplicant();
        StandardApplicant alternativeApplicant = new StandardApplicant();
        PayloadForGet payload = PayloadForGet.builder().date(LocalDate.now()).code(code).build();
        when(standardApplicantRepository.findStandardApplicantByCodeAndDate(code, localDate))
                .thenReturn(List.of(standardApplicant, alternativeApplicant));

        LogCaptor logCaptor = LogCaptor.forClass(ReferenceDataSelectionUtil.class);

        StandardApplicant actual = validator.validate(payload, (request, applicant) -> applicant);

        Assertions.assertSame(standardApplicant, actual);
        Assertions.assertTrue(logCaptor.getWarnLogs().getFirst().contains("Data quality warning"));
    }
}
