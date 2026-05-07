package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
public class GetApplicationListEntriesValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @InjectMocks private GetApplicationListEntriesValidator getApplicationListEntriesValidator;

    @Test
    void testSuccessfulValidation() {
        UUID listId = UUID.randomUUID();

        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        // mock core database interaction for success
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuid(listId)).thenReturn(Optional.of(applicationList));

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        Assertions.assertThatNoException()
                .isThrownBy(
                        () -> getApplicationListEntriesValidator.validate(payloadGetEntryInList));
    }

    @Test
    void testValidationFailsWhenListNotFound() {
        UUID listId = UUID.randomUUID();

        when(applicationListRepository.findByUuid(listId)).thenReturn(Optional.empty());

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(UUID.randomUUID()).build();

        Assertions.assertThatException()
                .isThrownBy(
                        () -> getApplicationListEntriesValidator.validate(payloadGetEntryInList))
                .isInstanceOf(AppRegistryException.class)
                .withMessage("The application list with id %s was not found", listId);
    }
}
