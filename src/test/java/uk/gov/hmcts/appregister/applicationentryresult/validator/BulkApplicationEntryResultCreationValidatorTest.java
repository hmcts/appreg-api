package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkApplicationEntryResultCreationValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Mock private ResolutionCodeRepository resolutionCodeRepository;

    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private BulkApplicationEntryResultCreationValidator validator;

    @Test
    void testSuccessWithListProvided() {
        UUID appList = UUID.randomUUID();

        // setup 3 of the entries to belong to the list
        final UUID appEntry = UUID.randomUUID();
        final UUID appEntry2 = UUID.randomUUID();
        final UUID appEntry3 = UUID.randomUUID();

        var list = new ApplicationList();
        list.setUuid(appList);
        list.setStatus(Status.OPEN);
        when(applicationListRepository.findByUuid(appList)).thenReturn(Optional.of(list));

        ResultCreateDto resultCreateDto = new ResultCreateDto();
        resultCreateDto.setWordingFields(List.of(new TemplateSubstitution()));

        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(appEntry, appEntry2, appEntry3));
        bulkResultDto.setResult(resultCreateDto);

        when(applicationListEntryRepository.findByUuidsInSourceList(
                        eq(appList),
                        argThat(set -> set.containsAll(List.of(appEntry, appEntry2, appEntry3)))))
                .thenReturn(
                        List.of(
                                entry(appEntry, list),
                                entry(appEntry2, list),
                                entry(appEntry3, list)));

        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder()
                        .listId(appList)
                        .payload(bulkResultDto)
                        .build();

        // run the validation
        boolean success =
                validator.validate(
                        payloadForCreateEntryResult,
                        (v, r) -> {
                            Assertions.assertEquals(3, r.getResults().size());
                            Assertions.assertEquals(
                                    appList, r.getResults().get(0).payload().getListId());
                            Assertions.assertEquals(
                                    appList, r.getResults().get(1).payload().getListId());
                            Assertions.assertEquals(
                                    appList, r.getResults().get(2).payload().getListId());

                            // ensure that the entries are one of the ones we expect
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(
                                                    r.getResults().get(0).payload().getEntryId()));
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(
                                                    r.getResults().get(1).payload().getEntryId()));
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(
                                                    r.getResults().get(2).payload().getEntryId()));
                            return true;
                        });

        Assertions.assertTrue(success);
    }

    @Test
    void testSuccessWithNoListProvided() {
        UUID appList = UUID.randomUUID();
        UUID appList2 = UUID.randomUUID();

        // setup 3 of the entries to belong to the list
        final UUID appEntry = UUID.randomUUID();
        final UUID appEntry2 = UUID.randomUUID();
        final UUID appEntry3 = UUID.randomUUID();

        var list = new ApplicationList();
        list.setUuid(appList);
        list.setStatus(Status.OPEN);
        var list2 = new ApplicationList();
        list2.setUuid(appList2);
        list2.setStatus(Status.OPEN);

        ResultCreateDto resultCreateDto = new ResultCreateDto();
        resultCreateDto.setWordingFields(List.of(new TemplateSubstitution()));

        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(appEntry, appEntry2, appEntry3));
        bulkResultDto.setResult(resultCreateDto);

        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2, appEntry3));
        when(applicationListEntryRepository.findActiveByUuids(argThat(uuidMatcher)))
                .thenReturn(
                        List.of(
                                entry(appEntry, list),
                                entry(appEntry2, list2),
                                entry(appEntry3, list2)));

        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder().payload(bulkResultDto).build();

        // run the validation
        boolean success =
                validator.validate(
                        payloadForCreateEntryResult,
                        (v, r) -> {
                            Assertions.assertEquals(3, r.getResults().size());
                            Assertions.assertEquals(
                                    appList, r.getResults().get(0).payload().getListId());
                            Assertions.assertEquals(
                                    appList2, r.getResults().get(1).payload().getListId());
                            Assertions.assertEquals(
                                    appList2, r.getResults().get(2).payload().getListId());
                            Assertions.assertEquals(
                                    appEntry, r.getResults().get(0).payload().getEntryId());
                            Assertions.assertEquals(
                                    appEntry2, r.getResults().get(1).payload().getEntryId());
                            Assertions.assertEquals(
                                    appEntry3, r.getResults().get(2).payload().getEntryId());

                            return true;
                        });

        Assertions.assertTrue(success);
    }

    @Test
    void testFailureNoList() {
        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder().listId(UUID.randomUUID()).build();

        // run the validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            validator.validate(
                                    payloadForCreateEntryResult,
                                    (v, r) -> {
                                        return true;
                                    });
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(),
                exception.getCode().getCode());
    }

    @Test
    void testEntriesNotFoundForList() {
        UUID appList = UUID.randomUUID();

        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder()
                        .payload(new BulkResultDto())
                        .listId(appList)
                        .build();

        UUID appEntry = UUID.randomUUID();
        UUID appEntry2 = UUID.randomUUID();

        payloadForCreateEntryResult.getPayload().setEntryIds(List.of(appEntry, appEntry2));

        var list = new ApplicationList();
        list.setUuid(appList);
        list.setStatus(Status.OPEN);
        when(applicationListRepository.findByUuid(appList)).thenReturn(Optional.of(list));
        when(applicationListEntryRepository.findByUuidsInSourceList(
                        eq(appList), argThat(set -> set.containsAll(List.of(appEntry, appEntry2)))))
                .thenReturn(List.of(entry(appEntry, list)));
        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2));
        when(applicationListEntryRepository.findActiveByUuids(argThat(uuidMatcher)))
                .thenReturn(List.of(entry(appEntry, list), entry(appEntry2, list)));

        // run the validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            validator.validate(
                                    payloadForCreateEntryResult,
                                    (v, r) -> {
                                        return true;
                                    });
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_ENTRIES_NOT_IN_LIST
                        .getCode(),
                exception.getCode().getCode());
    }

    @Test
    void testMissingEntryForListReturnsEntryDoesNotExist() {
        UUID appList = UUID.randomUUID();

        var payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder()
                        .payload(new BulkResultDto())
                        .listId(appList)
                        .build();

        UUID appEntry = UUID.randomUUID();
        UUID missingEntry = UUID.randomUUID();

        payloadForCreateEntryResult.getPayload().setEntryIds(List.of(appEntry, missingEntry));

        var list = new ApplicationList();
        list.setUuid(appList);
        list.setStatus(Status.OPEN);
        when(applicationListRepository.findByUuid(appList)).thenReturn(Optional.of(list));
        when(applicationListEntryRepository.findByUuidsInSourceList(
                        eq(appList),
                        argThat(set -> set.containsAll(List.of(appEntry, missingEntry)))))
                .thenReturn(List.of(entry(appEntry, list)));
        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, missingEntry));
        when(applicationListEntryRepository.findActiveByUuids(argThat(uuidMatcher)))
                .thenReturn(List.of(entry(appEntry, list)));

        var exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                validator.validate(
                                        payloadForCreateEntryResult,
                                        (v, r) -> {
                                            return true;
                                        }));

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(),
                exception.getCode().getCode());
    }

    @Test
    void testNoListFoundForAllEntries() {
        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder()
                        .payload(new BulkResultDto())
                        .build();

        UUID appEntry = UUID.randomUUID();
        UUID appEntry2 = UUID.randomUUID();

        payloadForCreateEntryResult.getPayload().setEntryIds(List.of(appEntry, appEntry2));
        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2));

        var list = new ApplicationList();
        list.setUuid(UUID.randomUUID());
        list.setStatus(Status.OPEN);
        when(applicationListEntryRepository.findActiveByUuids(argThat(uuidMatcher)))
                .thenReturn(List.of(entry(UUID.randomUUID(), list)));

        // run the validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            validator.validate(
                                    payloadForCreateEntryResult,
                                    (v, r) -> {
                                        return true;
                                    });
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST.getCode(),
                exception.getCode().getCode());
    }

    @Test
    void givenDuplicateEntryIds_whenValidate_thenThrowsEntryIdsMustBeUnique() {
        UUID entryId = UUID.randomUUID();
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entryId, entryId));
        bulkResultDto.setResult(new ResultCreateDto());

        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder()
                        .listId(UUID.randomUUID())
                        .payload(bulkResultDto)
                        .build();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payloadForCreateEntryResult));

        Assertions.assertEquals(ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE, exception.getCode());
    }

    @Test
    void givenMissingEntryIds_whenValidate_thenThrowsEntryNotProvided() {
        var bulkResultDto = new BulkResultDto();
        bulkResultDto.setResult(new ResultCreateDto());

        var payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder().payload(bulkResultDto).build();

        var exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payloadForCreateEntryResult));

        Assertions.assertEquals(ApplicationListError.ENTRY_NOT_PROVIDED, exception.getCode());
    }

    @Test
    void givenMissingPayload_whenValidate_thenThrowsEntryNotProvided() {
        var payloadForCreateEntryResult = PayloadForCreateResults.<BulkResultDto>builder().build();

        var exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payloadForCreateEntryResult));

        Assertions.assertEquals(ApplicationListError.ENTRY_NOT_PROVIDED, exception.getCode());
    }

    /**
     * A matcher that allows us to check that a list matches an expected list regardless of order.
     */
    @RequiredArgsConstructor
    public class OrderIndependantUuidMatcher implements ArgumentMatcher<List<UUID>> {

        private final List<UUID> expected;

        // constructors

        @Override
        public boolean matches(List<UUID> uuid) {
            for (UUID e : expected) {
                if (!expected.contains(e)) {
                    return false;
                }
            }
            return true;
        }
    }

    private ApplicationListEntry entry(UUID entryId, ApplicationList list) {
        var entry = new ApplicationListEntry();
        entry.setUuid(entryId);
        entry.setApplicationList(list);
        return entry;
    }
}
