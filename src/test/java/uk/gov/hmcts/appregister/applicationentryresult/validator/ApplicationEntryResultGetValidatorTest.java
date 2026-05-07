package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;

@ExtendWith(MockitoExtension.class)
public class ApplicationEntryResultGetValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private ResolutionCodeRepository resolutionCodeRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private ApplicationEntryResultGetValidator validator;

    @Test
    void testGetApplicationListEntryResultForEntryAndListSuccess() {
        PayloadGetEntryResultInList payload =
                PayloadGetEntryResultInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // make the list open
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        Mockito.when(applicationListRepository.findByUuidIncludingDelete(payload.getListId()))
                .thenReturn(java.util.Optional.of(applicationList));

        Mockito.when(
                        applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                                payload.getEntryId(), payload.getListId()))
                .thenReturn(java.util.Optional.of(new ApplicationListEntry()));

        // test
        validator.validate(payload);

        // assert the expectations
        Mockito.verify(applicationListRepository).findByUuidIncludingDelete(payload.getListId());
        Mockito.verify(applicationListEntryRepository)
                .findActiveByUuidAndApplicationListUuid(payload.getEntryId(), payload.getListId());
    }

    @Test
    void testGetApplicationListEntryResultForEntryAndDeletedList() {
        PayloadGetEntryResultInList payload =
                PayloadGetEntryResultInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // make the list deleted
        ApplicationList applicationList = new ApplicationList();
        applicationList.setDeleted(YesOrNo.YES);

        Mockito.when(applicationListRepository.findByUuidIncludingDelete(payload.getListId()))
                .thenReturn(java.util.Optional.of(applicationList));

        // test
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));
        // assert the expectations
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(),
                appRegistryException.getCode().getCode());
    }

    @Test
    void testGetApplicationListEntryResultFailOnApplicationListNotFound() {
        PayloadGetEntryResultInList payload =
                PayloadGetEntryResultInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // test
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(),
                appRegistryException.getCode().getCode());
    }

    @Test
    void testGetApplicationListEntryResultFailOnApplicationListEntryNotFound() {
        PayloadGetEntryResultInList payload =
                PayloadGetEntryResultInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // make the list open
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        Mockito.when(applicationListRepository.findByUuidIncludingDelete(payload.getListId()))
                .thenReturn(java.util.Optional.of(applicationList));

        // test
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(),
                appRegistryException.getCode().getCode());
    }
}
