package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.val;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkUpdateResponseDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ApplicationEntryControllerBulkFeesTest extends AbstractApplicationEntryCrudTest {

    private static final LocalDate ORIGINAL_STATUS_DATE = LocalDate.of(2025, Month.JANUARY, 10);
    private static final LocalDate UPDATED_STATUS_DATE = LocalDate.of(2025, Month.OCTOBER, 7);
    private static final String ORIGINAL_PAYMENT_REFERENCE = "PAY-ORIGINAL";
    private static final String UPDATED_PAYMENT_REFERENCE = "PAY-UPDATED";

    @Test
    void givenBulkFeesRequestContainsUnsupportedNestedField_whenBulkUpdateFees_thenReturns400()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        BulkFeesUpdateDto request = validBulkFeesUpdateDto(Set.of(entry.getId()));
        ObjectNode requestBody = mapper.valueToTree(request);
        ArrayNode feeDetails = (ArrayNode) requestBody.path("feeDetails");
        ((ObjectNode) feeDetails.get(0)).put("unexpected", "value");

        Response response =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT + "/" + entry.getListId() + "/entries/fees"),
                        tokenGenerator.fetchTokenForRole(),
                        mapper.writeValueAsString(requestBody));

        response.then().statusCode(400);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getDetail())
                .isEqualTo("Unsupported request field: feeDetails[0].unexpected");
    }

    @Test
    void givenValidEntries_whenBulkUpdateFees_thenFeeStatusesAreAppendedForEveryEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto firstEntry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        EntryGetDetailDto secondEntry =
                createEntry(
                        Optional.of(firstEntry.getListId()),
                        PaymentStatus.DUE,
                        ORIGINAL_STATUS_DATE,
                        null,
                        false);

        differenceLogAsserter.clearLogs();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        firstEntry.getListId(),
                        validBulkFeesUpdateDto(Set.of(firstEntry.getId(), secondEntry.getId())));

        response.then().statusCode(200);
        BulkUpdateResponseDto responseDto = response.as(BulkUpdateResponseDto.class);
        assertThat(responseDto.getTotalCount()).isEqualTo(2);
        assertThat(responseDto.getUpdatedCount()).isEqualTo(2);
        assertThat(responseDto.getStatus()).isEqualTo(BulkUpdateResponseDto.StatusEnum.SUCCEEDED);
        assertSuccessfulBulkUpdateAudited();

        assertFeeDetails(
                getEntry(tokenGenerator, firstEntry.getListId(), firstEntry.getId()),
                true,
                feeStatus(PaymentStatus.PAID, ORIGINAL_STATUS_DATE, ORIGINAL_PAYMENT_REFERENCE),
                feeStatus(PaymentStatus.REMITTED, UPDATED_STATUS_DATE, UPDATED_PAYMENT_REFERENCE));
        assertFeeDetails(
                getEntry(tokenGenerator, firstEntry.getListId(), secondEntry.getId()),
                true,
                feeStatus(PaymentStatus.DUE, ORIGINAL_STATUS_DATE, null),
                feeStatus(PaymentStatus.REMITTED, UPDATED_STATUS_DATE, UPDATED_PAYMENT_REFERENCE));
    }

    @Test
    void givenValidEntriesAndMultipleFeeDetails_whenBulkUpdateFees_thenAllFeeDetailsAreAppended()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto firstEntry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        EntryGetDetailDto secondEntry =
                createEntry(
                        Optional.of(firstEntry.getListId()),
                        PaymentStatus.DUE,
                        ORIGINAL_STATUS_DATE,
                        null,
                        false);
        BulkFeesUpdateDto dto =
                new BulkFeesUpdateDto()
                        .entryIds(Set.of(firstEntry.getId(), secondEntry.getId()))
                        .feeDetails(
                                List.of(
                                        feeDetails(
                                                PaymentStatus.PAID,
                                                UPDATED_STATUS_DATE,
                                                "PAY-BULK-1",
                                                false),
                                        feeDetails(
                                                PaymentStatus.REMITTED,
                                                UPDATED_STATUS_DATE,
                                                "PAY-BULK-2",
                                                true)));

        differenceLogAsserter.clearLogs();

        Response response = bulkUpdateFees(tokenGenerator, firstEntry.getListId(), dto);

        response.then().statusCode(200);
        BulkUpdateResponseDto responseDto = response.as(BulkUpdateResponseDto.class);
        assertThat(responseDto.getTotalCount()).isEqualTo(2);
        assertThat(responseDto.getUpdatedCount()).isEqualTo(2);
        assertThat(responseDto.getStatus()).isEqualTo(BulkUpdateResponseDto.StatusEnum.SUCCEEDED);

        assertFeeDetails(
                getEntry(tokenGenerator, firstEntry.getListId(), firstEntry.getId()),
                true,
                feeStatus(PaymentStatus.PAID, ORIGINAL_STATUS_DATE, ORIGINAL_PAYMENT_REFERENCE),
                feeStatus(PaymentStatus.PAID, UPDATED_STATUS_DATE, "PAY-BULK-1"),
                feeStatus(PaymentStatus.REMITTED, UPDATED_STATUS_DATE, "PAY-BULK-2"));
        assertFeeDetails(
                getEntry(tokenGenerator, firstEntry.getListId(), secondEntry.getId()),
                true,
                feeStatus(PaymentStatus.DUE, ORIGINAL_STATUS_DATE, null),
                feeStatus(PaymentStatus.PAID, UPDATED_STATUS_DATE, "PAY-BULK-1"),
                feeStatus(PaymentStatus.REMITTED, UPDATED_STATUS_DATE, "PAY-BULK-2"));
    }

    @Test
    void
            givenEntryAlreadyHasOffsiteFee_whenBulkUpdateFeesWithoutOffsiteFlag_thenOffsiteFeeIsPreserved()
                    throws Exception {
        val tokenGenerator = createAdminToken();
        val entry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        true);

        differenceLogAsserter.clearLogs();

        val response =
                bulkUpdateFees(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkFeesUpdateDto()
                                .entryIds(Set.of(entry.getId()))
                                .feeDetails(
                                        List.of(
                                                feeDetails(
                                                        PaymentStatus.REMITTED,
                                                        UPDATED_STATUS_DATE,
                                                        UPDATED_PAYMENT_REFERENCE,
                                                        false))));

        response.then().statusCode(200);

        assertFeeDetails(
                getEntry(tokenGenerator, entry.getListId(), entry.getId()),
                true,
                feeStatus(PaymentStatus.PAID, ORIGINAL_STATUS_DATE, ORIGINAL_PAYMENT_REFERENCE),
                feeStatus(PaymentStatus.REMITTED, UPDATED_STATUS_DATE, UPDATED_PAYMENT_REFERENCE));
    }

    @Test
    void givenMissingEntry_whenBulkUpdateFees_thenReturns400AndDoesNotUpdateAnyEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        UUID missingEntryId = UUID.randomUUID();

        differenceLogAsserter.clearLogs();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        entry.getListId(),
                        validBulkFeesUpdateDto(Set.of(entry.getId(), missingEntryId)));

        response.then().statusCode(400);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST.getCode().getAppCode());
        assertThat(problemDetail.getDetail()).contains(missingEntryId.toString());
        assertNoBulkFeeAuditWritten();
        assertFeeDetails(
                getEntry(tokenGenerator, entry.getListId(), entry.getId()),
                PaymentStatus.PAID,
                ORIGINAL_STATUS_DATE,
                ORIGINAL_PAYMENT_REFERENCE,
                false);
    }

    @Test
    void givenEntryFromAnotherList_whenBulkUpdateFees_thenReturns403AndDoesNotUpdateAnyEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto sourceEntry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        EntryGetDetailDto otherListEntry =
                createEntry(Optional.empty(), PaymentStatus.DUE, ORIGINAL_STATUS_DATE, null, false);

        differenceLogAsserter.clearLogs();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        sourceEntry.getListId(),
                        validBulkFeesUpdateDto(
                                Set.of(sourceEntry.getId(), otherListEntry.getId())));

        response.then().statusCode(403);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST.getCode().getAppCode());
        assertThat(problemDetail.getStatus()).isEqualTo(403);
        assertThat(problemDetail.getTitle())
                .isEqualTo(
                        "One or more application list entries do not belong to the application list");
        assertThat(problemDetail.getDetail()).contains(otherListEntry.getId().toString());
        assertNoBulkFeeAuditWritten();
        assertFeeDetails(
                getEntry(tokenGenerator, sourceEntry.getListId(), sourceEntry.getId()),
                PaymentStatus.PAID,
                ORIGINAL_STATUS_DATE,
                ORIGINAL_PAYMENT_REFERENCE,
                false);
        assertFeeDetails(
                getEntry(tokenGenerator, otherListEntry.getListId(), otherListEntry.getId()),
                PaymentStatus.DUE,
                ORIGINAL_STATUS_DATE,
                null,
                false);
    }

    @Test
    void givenClosedApplicationList_whenBulkUpdateFees_thenReturns409() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        UUID closedListId = getClosedApplicationListId();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        closedListId,
                        validBulkFeesUpdateDto(Set.of(UUID.randomUUID())));

        response.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(), response);
    }

    @Test
    void givenInvalidFeeDetails_whenBulkUpdateFees_thenReturns400AndDoesNotUpdateEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);
        BulkFeeDetailsDto feeDetails =
                feeDetails(
                        PaymentStatus.REMITTED,
                        LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1),
                        UPDATED_PAYMENT_REFERENCE,
                        true);

        differenceLogAsserter.clearLogs();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        entry.getListId(),
                        new BulkFeesUpdateDto()
                                .entryIds(Set.of(entry.getId()))
                                .feeDetails(List.of(feeDetails)));

        response.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.FEE_STATUS_DATE_CANNOT_BE_IN_FUTURE.getCode(), response);
        assertNoBulkFeeAuditWritten();
        assertFeeDetails(
                getEntry(tokenGenerator, entry.getListId(), entry.getId()),
                PaymentStatus.PAID,
                ORIGINAL_STATUS_DATE,
                ORIGINAL_PAYMENT_REFERENCE,
                false);
    }

    @Test
    void givenTooManyEntryIds_whenBulkUpdateFees_thenReturns400AndDoesNotUpdateEntry()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        EntryGetDetailDto entry =
                createEntry(
                        Optional.empty(),
                        PaymentStatus.PAID,
                        ORIGINAL_STATUS_DATE,
                        ORIGINAL_PAYMENT_REFERENCE,
                        false);

        differenceLogAsserter.clearLogs();

        Response response =
                bulkUpdateFees(
                        tokenGenerator,
                        entry.getListId(),
                        validBulkFeesUpdateDto(entryIdsIncluding(entry.getId(), 501)));

        response.then().statusCode(400);
        assertNoBulkFeeAuditWritten();
        assertFeeDetails(
                getEntry(tokenGenerator, entry.getListId(), entry.getId()),
                PaymentStatus.PAID,
                ORIGINAL_STATUS_DATE,
                ORIGINAL_PAYMENT_REFERENCE,
                false);
    }

    private EntryGetDetailDto createEntry(
            Optional<UUID> listId,
            PaymentStatus paymentStatus,
            LocalDate statusDate,
            String paymentReference,
            boolean hasOffsiteFee)
            throws Exception {
        Response response =
                createListEntryWithAllData(
                        listId,
                        dto -> {
                            dto.setFeeStatuses(
                                    java.util.List.of(
                                            feeStatus(
                                                    paymentStatus, statusDate, paymentReference)));
                            dto.setHasOffsiteFee(hasOffsiteFee);
                        });
        response.then().statusCode(201);
        return response.as(EntryGetDetailDto.class);
    }

    private Response bulkUpdateFees(
            TokenGenerator tokenGenerator, UUID listId, BulkFeesUpdateDto dto) throws Exception {
        return restAssuredClient.executePutRequest(
                getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/fees"),
                tokenGenerator.fetchTokenForRole(),
                dto);
    }

    private void assertSuccessfulBulkUpdateAudited() {
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_FEE_STATUS,
                        "alefs_fee_status",
                        null,
                        "REMITTED",
                        AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY.getType().name(),
                        AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY.getEventName()));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLCATION_LISTS_ENTRY_FEE_ID,
                        "fee_fee_id",
                        null,
                        "",
                        AppListEntryAuditOperation.CREATE_FEE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.CREATE_FEE_ENTRY.getEventName()));
    }

    private void assertNoBulkFeeAuditWritten() {
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(0, true);
        differenceLogAsserter.assertDiffCount(0, false);
    }

    private EntryGetDetailDto getEntry(TokenGenerator tokenGenerator, UUID listId, UUID entryId)
            throws Exception {
        Response response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/" + entryId),
                        tokenGenerator.fetchTokenForRole());
        response.then().statusCode(200);
        return response.as(EntryGetDetailDto.class);
    }

    private BulkFeesUpdateDto validBulkFeesUpdateDto(Set<UUID> entryIds) {
        return new BulkFeesUpdateDto()
                .entryIds(entryIds)
                .feeDetails(
                        List.of(
                                feeDetails(
                                        PaymentStatus.REMITTED,
                                        UPDATED_STATUS_DATE,
                                        UPDATED_PAYMENT_REFERENCE,
                                        true)));
    }

    private Set<UUID> entryIdsIncluding(UUID entryId, int totalCount) {
        Set<UUID> entryIds = new LinkedHashSet<>();
        entryIds.add(entryId);

        for (long index = 1; entryIds.size() < totalCount; index++) {
            entryIds.add(new UUID(0L, index));
        }

        return entryIds;
    }

    private BulkFeeDetailsDto feeDetails(
            PaymentStatus paymentStatus,
            LocalDate statusDate,
            String paymentReference,
            boolean hasOffsiteFee) {
        return new BulkFeeDetailsDto()
                .paymentStatus(paymentStatus)
                .statusDate(statusDate)
                .paymentReference(paymentReference)
                .hasOffsiteFee(hasOffsiteFee);
    }

    private FeeStatus feeStatus(
            PaymentStatus paymentStatus, LocalDate statusDate, String paymentReference) {
        return new FeeStatus()
                .paymentStatus(paymentStatus)
                .statusDate(statusDate)
                .paymentReference(paymentReference);
    }

    private void assertFeeDetails(
            EntryGetDetailDto entry,
            PaymentStatus expectedStatus,
            LocalDate expectedStatusDate,
            String expectedPaymentReference,
            boolean expectedHasOffsiteFee) {
        assertThat(entry.getFeeStatuses()).hasSize(1);
        FeeStatus feeStatus = entry.getFeeStatuses().getFirst();
        assertThat(feeStatus.getPaymentStatus()).isEqualTo(expectedStatus);
        assertThat(feeStatus.getStatusDate()).isEqualTo(expectedStatusDate);
        assertThat(feeStatus.getPaymentReference()).isEqualTo(expectedPaymentReference);
        assertThat(entry.getHasOffsiteFee()).isEqualTo(expectedHasOffsiteFee);
    }

    private void assertFeeDetails(
            EntryGetDetailDto entry, boolean expectedHasOffsiteFee, FeeStatus... expectedStatuses) {
        assertThat(entry.getFeeStatuses())
                .extracting(
                        FeeStatus::getPaymentStatus,
                        FeeStatus::getStatusDate,
                        FeeStatus::getPaymentReference)
                .containsExactlyInAnyOrder(
                        Arrays.stream(expectedStatuses)
                                .map(
                                        status ->
                                                tuple(
                                                        status.getPaymentStatus(),
                                                        status.getStatusDate(),
                                                        status.getPaymentReference()))
                                .toArray(Tuple[]::new));
        assertThat(entry.getHasOffsiteFee()).isEqualTo(expectedHasOffsiteFee);
    }
}
