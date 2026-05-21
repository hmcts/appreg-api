package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateFeesPayload;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkUpdateFeesValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 10, 7);

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    private BulkUpdateFeesValidator validator;

    private UUID listId;
    private UUID entryId;
    private ApplicationList applicationList;
    private ApplicationListEntry applicationListEntry;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        applicationList = new AppListTestData().someMinimal().status(Status.OPEN).build();
        applicationList.setDeleted(null);

        applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setUuid(entryId);

        validator =
                new BulkUpdateFeesValidator(
                        applicationListRepository,
                        applicationListEntryRepository,
                        businessDateProvider,
                        500);

        when(businessDateProvider.currentUkDate()).thenReturn(TODAY);
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(listId, Set.of(entryId)))
                .thenReturn(List.of(applicationListEntry));
    }

    @Test
    void validateWithoutCallback_whenPayloadIsValid_thenCompletes() {
        validator.validate(validPayload(entryId));
    }

    @Test
    void validateWithCallback_whenPayloadIsValid_thenReturnsMatchingEntries() {
        List<ApplicationListEntry> entries =
                validator.validate(
                        validPayload(entryId), (request, success) -> success.getEntries());

        assertThat(entries).containsExactly(applicationListEntry);
    }

    @Test
    void validate_whenApplicationListDoesNotExist_thenThrowsApplicationListDoesNotExist() {
        UUID missingListId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIncludingDelete(missingListId))
                .thenReturn(Optional.empty());
        BulkUpdateFeesPayload payload = validPayloadForList(missingListId, entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST);
    }

    @Test
    void validate_whenApplicationListIsClosed_thenThrowsApplicationListStateIsIncorrect() {
        applicationList.setStatus(Status.CLOSED);
        BulkUpdateFeesPayload payload = validPayload(entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT);
    }

    @Test
    void validate_whenApplicationListIsDeleted_thenThrowsApplicationListStateIsIncorrect() {
        applicationList.setDeleted(true);
        BulkUpdateFeesPayload payload = validPayload(entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT);
    }

    @Test
    void validate_whenEntryIdsAreMissing_thenThrowsEntryNotProvided() {
        BulkUpdateFeesPayload payload =
                new BulkUpdateFeesPayload(
                        listId, new BulkFeesUpdateDto().feeDetails(validFeeDetails()));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_PROVIDED);
    }

    @Test
    void validate_whenEntryIdsExceedLimit_thenThrowsTooManyEntries() {
        BulkUpdateFeesPayload payload = validPayload(listId, entryIds(501), validFeeDetails());

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_FEE_UPDATE_TOO_MANY_ENTRIES);
        assertThat(exception.getDetails()).containsEntry("max_entry_ids", "500");
    }

    @Test
    void validate_whenFeeDetailsAreMissing_thenThrowsFeeDetailsNotProvided() {
        BulkUpdateFeesPayload payload =
                new BulkUpdateFeesPayload(
                        listId, new BulkFeesUpdateDto().entryIds(Set.of(entryId)));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.FEE_DETAILS_NOT_PROVIDED);
    }

    @Test
    void validate_whenPaymentStatusIsMissing_thenThrowsPaymentStatusRequired() {
        BulkFeeDetailsDto feeDetails = validFeeDetails();
        feeDetails.setPaymentStatus(null);

        AppRegistryException exception = validateAndCapture(validPayload(entryId, feeDetails));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.FEE_PAYMENT_STATUS_REQUIRED);
    }

    @Test
    void validate_whenStatusDateIsMissing_thenThrowsStatusDateRequired() {
        BulkFeeDetailsDto feeDetails = validFeeDetails();
        feeDetails.setStatusDate(null);

        AppRegistryException exception = validateAndCapture(validPayload(entryId, feeDetails));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.FEE_STATUS_DATE_REQUIRED);
    }

    @Test
    void validate_whenStatusDateIsInTheFuture_thenThrowsStatusDateCannotBeInFuture() {
        BulkFeeDetailsDto feeDetails = validFeeDetails();
        feeDetails.setStatusDate(TODAY.plusDays(1));

        AppRegistryException exception = validateAndCapture(validPayload(entryId, feeDetails));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.FEE_STATUS_DATE_CANNOT_BE_IN_FUTURE);
    }

    @Test
    void validate_whenPaymentReferenceIsTooLong_thenThrowsPaymentReferenceTooLong() {
        BulkFeeDetailsDto feeDetails = validFeeDetails();
        feeDetails.setPaymentReference("a".repeat(101));

        AppRegistryException exception = validateAndCapture(validPayload(entryId, feeDetails));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.PAYMENT_REFERENCE_TOO_LONG);
    }

    @Test
    void validate_whenHasOffsiteFeeIsMissing_thenThrowsOffsiteFeeRequired() {
        BulkFeeDetailsDto feeDetails = validFeeDetails();
        feeDetails.setHasOffsiteFee(null);

        AppRegistryException exception = validateAndCapture(validPayload(entryId, feeDetails));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFSITE_FEE_REQUIRED);
    }

    @Test
    void validate_whenSomeEntriesAreNotInSourceList_thenThrowsEntryNotInSourceList() {
        UUID missingEntryId = UUID.randomUUID();
        when(applicationListEntryRepository.findByUuidsInSourceList(
                        listId, Set.of(entryId, missingEntryId)))
                .thenReturn(List.of(applicationListEntry));
        when(applicationListEntryRepository.findApplicationListForAllEntries(anyList()))
                .thenReturn(List.of(new EntryToList(entryId, listId)));

        BulkUpdateFeesPayload payload = validPayload(entryId, missingEntryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST);
        assertThat(exception.getDetails().get("invalid_entry_ids"))
                .contains(missingEntryId.toString());
    }

    @Test
    void validate_whenSomeEntriesBelongToAnotherList_thenThrowsEntryNotAccessibleForList() {
        UUID otherListEntryId = UUID.randomUUID();
        UUID otherListId = UUID.randomUUID();
        when(applicationListEntryRepository.findByUuidsInSourceList(
                        listId, Set.of(entryId, otherListEntryId)))
                .thenReturn(List.of(applicationListEntry));
        when(applicationListEntryRepository.findApplicationListForAllEntries(anyList()))
                .thenReturn(
                        List.of(
                                new EntryToList(entryId, listId),
                                new EntryToList(otherListEntryId, otherListId)));

        BulkUpdateFeesPayload payload = validPayload(entryId, otherListEntryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST);
        assertThat(exception.getDetails().get("invalid_entry_ids"))
                .contains(otherListEntryId.toString());
    }

    private BulkUpdateFeesPayload validPayload(UUID... entryIds) {
        return validPayload(listId, Set.of(entryIds), validFeeDetails());
    }

    private BulkUpdateFeesPayload validPayload(UUID entryId, BulkFeeDetailsDto feeDetails) {
        return validPayload(listId, Set.of(entryId), feeDetails);
    }

    private BulkUpdateFeesPayload validPayload(
            UUID listId, Set<UUID> entryIds, BulkFeeDetailsDto feeDetails) {
        return new BulkUpdateFeesPayload(
                listId, new BulkFeesUpdateDto().entryIds(entryIds).feeDetails(feeDetails));
    }

    private BulkUpdateFeesPayload validPayloadForList(UUID listId, UUID entryId) {
        return validPayload(listId, Set.of(entryId), validFeeDetails());
    }

    private Set<UUID> entryIds(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> UUID.randomUUID())
                .collect(Collectors.toSet());
    }

    private AppRegistryException validateAndCapture(BulkUpdateFeesPayload payload) {
        return assertThrows(AppRegistryException.class, () -> validator.validate(payload));
    }

    private BulkFeeDetailsDto validFeeDetails() {
        return new BulkFeeDetailsDto()
                .paymentStatus(PaymentStatus.PAID)
                .statusDate(TODAY)
                .paymentReference("PAY-001")
                .hasOffsiteFee(false);
    }
}
