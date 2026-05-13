package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BulkApplicationEntryResultCreationValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private BulkApplicationEntryResultCreationValidator validator;

    @Test
    void testSuccessWithListProvided() {
        UUID appList = UUID.randomUUID();

        // setup 3 of the entries to belong to the list
        UUID appEntry = UUID.randomUUID();
        UUID appEntry2 = UUID.randomUUID();
        UUID appEntry3 = UUID.randomUUID();

        when(applicationListRepository.findByUuid(appList))
                .thenReturn(Optional.of(new ApplicationList()));

        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2, appEntry3));
        when(applicationListEntryRepository.doesApplicationEntryBelongToApplicationList(
                        argThat(uuidMatcher), eq(appList)))
                .thenReturn(true);

        ResultCreateDto resultCreateDto = new ResultCreateDto();
        resultCreateDto.setResultCode("code");
        resultCreateDto.setWordingFields(List.of(new TemplateSubstitution()));

        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setResult(resultCreateDto);

        bulkResultDto.setEntryIds(Set.of(appEntry, appEntry2, appEntry3));
        bulkResultDto.setResult(new ResultCreateDto());

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
                            ;
                            Assertions.assertEquals(3, r.getResults().size());
                            Assertions.assertEquals(appList, r.getResults().get(0).getListId());
                            Assertions.assertEquals(appList, r.getResults().get(1).getListId());
                            Assertions.assertEquals(appList, r.getResults().get(2).getListId());

                            // ensure that the entries are one of the ones we expect
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(r.getResults().get(0).getEntryId()));
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(r.getResults().get(1).getEntryId()));
                            Assertions.assertTrue(
                                    Set.of(appEntry, appEntry2, appEntry3)
                                            .contains(r.getResults().get(2).getEntryId()));
                            return true;
                        });

        Assertions.assertTrue(success);
    }

    @Test
    void testSuccessWithNoListProvided() {
        UUID appList = UUID.randomUUID();
        UUID appList2 = UUID.randomUUID();

        // setup 3 of the entries to belong to the list
        UUID appEntry = UUID.randomUUID();
        UUID appEntry2 = UUID.randomUUID();
        UUID appEntry3 = UUID.randomUUID();

        List<EntryToList> entryToLists = new ArrayList<>();

        // these are the entry to lists that we expect to be returned from the db
        EntryToList entryToList = new EntryToList(appEntry, appList);
        EntryToList entryToList2 = new EntryToList(appEntry2, appList2);
        EntryToList entryToList3 = new EntryToList(appEntry3, appList2);

        entryToLists.add(entryToList);
        entryToLists.add(entryToList2);
        entryToLists.add(entryToList3);

        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2, appEntry3));

        when(applicationListEntryRepository.findApplicationListForAllEntries(argThat(uuidMatcher)))
                .thenReturn(entryToLists);

        ResultCreateDto resultCreateDto = new ResultCreateDto();
        resultCreateDto.setResultCode("code");
        resultCreateDto.setWordingFields(List.of(new TemplateSubstitution()));

        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setResult(resultCreateDto);

        bulkResultDto.setEntryIds(Set.of(appEntry, appEntry2, appEntry3));
        bulkResultDto.setResult(new ResultCreateDto());

        PayloadForCreateResults<BulkResultDto> payloadForCreateEntryResult =
                PayloadForCreateResults.<BulkResultDto>builder().payload(bulkResultDto).build();

        // run the validation
        boolean success =
                validator.validate(
                        payloadForCreateEntryResult,
                        (v, r) -> {
                            ;
                            Assertions.assertEquals(3, r.getResults().size());
                            Assertions.assertEquals(appList, r.getResults().get(0).getListId());
                            Assertions.assertEquals(appList2, r.getResults().get(1).getListId());
                            Assertions.assertEquals(appList2, r.getResults().get(2).getListId());
                            Assertions.assertEquals(appEntry, r.getResults().get(0).getEntryId());
                            Assertions.assertEquals(appEntry2, r.getResults().get(1).getEntryId());
                            Assertions.assertEquals(appEntry3, r.getResults().get(2).getEntryId());

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
                                        ;
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

        payloadForCreateEntryResult.getPayload().setEntryIds(Set.of(appEntry, appEntry2));

        when(applicationListRepository.findByUuid(appList))
                .thenReturn(Optional.of(new ApplicationList()));

        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2));
        when(applicationListEntryRepository.doesApplicationEntryBelongToApplicationList(
                        argThat(uuidMatcher), eq(appList)))
                .thenReturn(false);

        // run the validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            validator.validate(
                                    payloadForCreateEntryResult,
                                    (v, r) -> {
                                        ;
                                        return true;
                                    });
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_ENTRIES_NOT_IN_LIST
                        .getCode(),
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

        payloadForCreateEntryResult.getPayload().setEntryIds(Set.of(appEntry, appEntry2));
        OrderIndependantUuidMatcher uuidMatcher =
                new OrderIndependantUuidMatcher(List.of(appEntry, appEntry2));

        // return only one entry so we error
        when(applicationListEntryRepository.findApplicationListForAllEntries(argThat(uuidMatcher)))
                .thenReturn(List.of(new EntryToList(UUID.randomUUID(), UUID.randomUUID())));

        // run the validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            validator.validate(
                                    payloadForCreateEntryResult,
                                    (v, r) -> {
                                        ;
                                        return true;
                                    });
                        });

        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST.getCode(),
                exception.getCode().getCode());
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
}
