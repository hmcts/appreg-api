package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.model.MoveEntriesPayload;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@ExtendWith(MockitoExtension.class)
public class MoveEntriesValidatorTest {

    @Mock private ApplicationListRepository alRepository;

    @InjectMocks private MoveEntriesValidator validator;

    private UUID sourceListId;
    private UUID targetListId;

    @BeforeEach
    void setUp() {
        sourceListId = UUID.randomUUID();
        targetListId = UUID.randomUUID();
    }

    @Test
    void validate_successful_whenValidRequest() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID(), UUID.randomUUID()));

        MoveEntriesValidationSuccess success =
                validator.validate(payload(sourceListId, dto), (d, s) -> s);

        Assertions.assertNotNull(success);
        Assertions.assertEquals(target, success.getTargetList());
    }

    @Test
    void validate_throws_invalidListStatus_whenSourceListNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(CLOSED);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dto), (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("source list")
                .hasMessageContaining(sourceListId.toString());
    }

    @Test
    void validate_throws_invalidListStatus_whenTargetListNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(CLOSED);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dto), (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("target list")
                .hasMessageContaining(targetListId.toString());
    }

    @Test
    void validate_throws_invalidListStatus_whenBothListsNotOpen() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(CLOSED);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(CLOSED);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dto), (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.INVALID_LIST_STATUS,
                                        ((AppRegistryException) ex).getCode()))
                .hasMessageContaining("source list")
                .hasMessageContaining("target list")
                .hasMessageContaining(sourceListId.toString())
                .hasMessageContaining(targetListId.toString());
    }

    @Test
    void validate_throws_notFound_whenSourceListMissing() {
        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dto), (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_notFound_whenTargetListMissing() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.empty());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dto), (d, s) -> s))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.TARGET_LIST_NOT_FOUND,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void validate_throws_entryNotProvided_whenEntryIdsNullOrEmpty() {
        ApplicationList source = new ApplicationList();
        source.setUuid(sourceListId);
        source.setStatus(OPEN);

        ApplicationList target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(sourceListId)).thenReturn(Optional.of(source));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        MoveEntriesDto dtoNull = new MoveEntriesDto();
        dtoNull.setTargetListId(targetListId);
        dtoNull.setEntryIds(null);

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dtoNull), (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_PROVIDED,
                                        ((AppRegistryException) ex).getCode()));

        MoveEntriesDto dtoEmpty = new MoveEntriesDto();
        dtoEmpty.setTargetListId(targetListId);
        dtoEmpty.setEntryIds(Set.of());

        assertThatThrownBy(() -> validator.validate(payload(sourceListId, dtoEmpty), (d, s) -> s))
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_PROVIDED,
                                        ((AppRegistryException) ex).getCode()));
    }

    /**
     * Summary: This regression test checks that concurrent requests cannot race on the validator
     * input and cause a CLOSED source list to be treated as valid.
     */
    @Test
    void validate_arcpoc_1249_shouldRejectClosedSource_whenAnotherThreadOverwritesSourceListId() {
        var closedSourceListId = UUID.randomUUID();
        var openSourceListId = UUID.randomUUID();

        var closedSource = new ApplicationList();
        closedSource.setUuid(closedSourceListId);
        closedSource.setStatus(CLOSED);

        var openSource = new ApplicationList();
        openSource.setUuid(openSourceListId);
        openSource.setStatus(OPEN);

        var target = new ApplicationList();
        target.setUuid(targetListId);
        target.setStatus(OPEN);

        when(alRepository.findByUuid(closedSourceListId)).thenReturn(Optional.of(closedSource));
        when(alRepository.findByUuid(openSourceListId)).thenReturn(Optional.of(openSource));
        when(alRepository.findByUuid(targetListId)).thenReturn(Optional.of(target));

        var dto = new MoveEntriesDto();
        dto.setTargetListId(targetListId);
        dto.setEntryIds(Set.of(UUID.randomUUID()));
        var closedPayload = payload(closedSourceListId, dto);
        var openPayload = payload(openSourceListId, dto);

        // This latch is a one-way signal. The "closed" thread uses it to tell the "open" thread
        // that the concurrent setup is ready and the OPEN validation can start.
        var closedRequestReady = new CountDownLatch(1);

        // This second signal keeps the "closed" thread paused until the "open" thread has
        // finished its own validation call. This recreates the old race ordering in a controlled
        // way while each request still keeps its own immutable payload.
        var openRequestComplete = new CountDownLatch(1);

        // A two-thread pool lets us run both requests at the same time without creating raw
        // Thread objects in the test.
        var executor = Executors.newFixedThreadPool(2);

        try {
            var closedRequest =
                    executor.submit(
                            () -> {
                                // This represents the forbidden request. It waits so the OPEN
                                // request can validate first, which mirrors the ordering that used
                                // to trigger the shared-state bug.
                                closedRequestReady.countDown();

                                // Wait until the "benign" request has completed its validation.
                                Assertions.assertTrue(
                                        openRequestComplete.await(5, TimeUnit.SECONDS));
                                return validator.validate(
                                        closedPayload, (request, success) -> success);
                            });

            var openRequest =
                    executor.submit(
                            () -> {
                                // Do not run this request until the forbidden request has started.
                                Assertions.assertTrue(
                                        closedRequestReady.await(5, TimeUnit.SECONDS));

                                try {
                                    // This request uses its own immutable payload, so it cannot
                                    // change the source list seen by the CLOSED request.
                                    return validator.validate(
                                            openPayload, (request, result) -> result);
                                } finally {
                                    // Always release the waiting thread so the test cannot hang if
                                    // this branch fails unexpectedly.
                                    openRequestComplete.countDown();
                                }
                            });

            var openSuccess =
                    Assertions.assertDoesNotThrow(() -> openRequest.get(5, TimeUnit.SECONDS));
            Assertions.assertEquals(target, openSuccess.getTargetList());

            // Even under concurrency, the CLOSED source list must still be resolved and rejected
            // with INVALID_LIST_STATUS.
            var ex =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> closedRequest.get(5, TimeUnit.SECONDS));
            var cause = ex.getCause();
            Assertions.assertInstanceOf(AppRegistryException.class, cause);
            Assertions.assertEquals(
                    ApplicationListError.INVALID_LIST_STATUS,
                    ((AppRegistryException) cause).getCode());
            verify(alRepository).findByUuid(closedSourceListId);
        } finally {
            executor.shutdownNow();
        }
    }

    private MoveEntriesPayload payload(UUID sourceListId, MoveEntriesDto moveEntriesDto) {
        return new MoveEntriesPayload(sourceListId, moveEntriesDto);
    }
}
