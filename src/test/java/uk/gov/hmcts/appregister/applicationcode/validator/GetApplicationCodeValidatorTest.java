package uk.gov.hmcts.appregister.applicationcode.validator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GetApplicationCodeValidatorTest {

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Test
    void testValidateSuccess() {
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        ApplicationCode applicationCode = applicationCodeTestData.someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        PayloadForGet payloadForGet =
                Instancio.of(PayloadForGet.class).withSettings(settings).create();

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(payloadForGet.getCode()), eq(payloadForGet.getDate())))
                .thenReturn(List.of(applicationCode));

        GetApplicationCodeValidator validator =
                new GetApplicationCodeValidator(applicationCodeRepository);
        validator.validate(payloadForGet);
    }

    @Test
    void testValidateFailureCodeDoesntExist() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        PayloadForGet payloadForGet =
                Instancio.of(PayloadForGet.class).withSettings(settings).create();

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(payloadForGet.getCode()), eq(payloadForGet.getDate())))
                .thenReturn(List.of());

        GetApplicationCodeValidator validator =
                new GetApplicationCodeValidator(applicationCodeRepository);

        // test
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payloadForGet));

        Assertions.assertEquals(ApplicationCodeError.CODE_NOT_FOUND, exception.getCode());
    }

    @Test
    void testValidateFailureDuplicateExists() {
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        ApplicationCode applicationCode = applicationCodeTestData.someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        PayloadForGet payloadForGet =
                Instancio.of(PayloadForGet.class).withSettings(settings).create();

        GetApplicationCodeValidator validator =
                new GetApplicationCodeValidator(applicationCodeRepository);

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(payloadForGet.getCode()), eq(payloadForGet.getDate())))
                .thenReturn(List.of(applicationCode, applicationCode));

        // test
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payloadForGet));

        // assert
        Assertions.assertEquals(ApplicationCodeError.DUPLICATE_CODE_FOUND, exception.getCode());
    }
}
