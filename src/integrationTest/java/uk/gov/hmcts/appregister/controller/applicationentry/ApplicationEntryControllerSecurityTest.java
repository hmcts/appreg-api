package uk.gov.hmcts.appregister.controller.applicationentry;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

class ApplicationEntryControllerSecurityTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-list-entries";
    private static final String CREATE_ENTRY_CONTEXT = "application-lists";
    protected static final String WEB_CONTEXT_UPDATE_CLOSED_ENTRY =
            "application-lists/%s/entries/closed/%s";
    protected static final String WEB_CONTEXT_ENTRY_FROM_CLOSED_LIST =
            "application-lists/%s/entries/closed/%s";
    private static final String DELETE_ENTRY_CONTEXT = "application-lists/%s/entries/%s";

    @Autowired private TransactionalUnitOfWork unitOfWork;
    @Autowired private ApplicationListRepository applicationListRepository;
    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        UUID[] validEntry = getValidEntryForList();
        UUID listId = validEntry[0];
        UUID entryId = validEntry[1];

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries"))
                        .method(HttpMethod.POST)
                        .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/bulk-action-preview"))
                        .method(HttpMethod.POST)
                        .payload(validBulkActionPreviewRequest(entryId))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/bulk-action-preview"))
                        .method(HttpMethod.POST)
                        .payload(validApplicationListBulkActionPreviewRequest(entryId))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/"
                                                + entryId))
                        .method(HttpMethod.PUT)
                        .payload(validEntryUpdateDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/"
                                                + entryId))
                        .method(HttpMethod.GET)
                        .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/move"))
                        .method(HttpMethod.POST)
                        .payload(
                                new MoveEntriesDto()
                                        .targetListId(UUID.randomUUID())
                                        .entryIds(Set.of(UUID.randomUUID())))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/fees"))
                        .method(HttpMethod.PUT)
                        .payload(validBulkFeesUpdateDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(DELETE_ENTRY_CONTEXT.formatted(listId, entryId)))
                        .method(HttpMethod.DELETE)
                        .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT_ENTRY_FROM_CLOSED_LIST.formatted(
                                                listId, entryId)))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT_UPDATE_CLOSED_ENTRY.formatted(listId, entryId)))
                        .method(HttpMethod.PUT)
                        .payload(new EntryUpdateClosedDto().additionalNotes("note"))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private BulkActionPreviewRequestDto validBulkActionPreviewRequest(UUID entryId) {
        return new BulkActionPreviewRequestDto()
                .action(BulkActionType.UPDATE_NOTES)
                .selection(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of(entryId)));
    }

    private ApplicationListEntryBulkActionPreviewRequestDto
            validApplicationListBulkActionPreviewRequest(UUID entryId) {
        return new ApplicationListEntryBulkActionPreviewRequestDto()
                .action(BulkActionType.UPDATE_FEE_DETAILS)
                .selection(
                        new ApplicationListEntryBulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of(entryId)));
    }

    private BulkFeesUpdateDto validBulkFeesUpdateDto() {
        return new BulkFeesUpdateDto()
                .entryIds(Set.of(UUID.randomUUID()))
                .feeDetails(
                        List.of(
                                new BulkFeeDetailsDto()
                                        .paymentStatus(PaymentStatus.PAID)
                                        .statusDate(LocalDate.now(java.time.ZoneOffset.UTC))
                                        .paymentReference("PAY-001")));
    }

    private UUID[] getValidEntryForList() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository.findAll().getFirst();
                    ApplicationListEntry applicationListEntry =
                            applicationListEntryRepository.findAll().stream()
                                    .filter(
                                            entry ->
                                                    entry.getApplicationList()
                                                            .getUuid()
                                                            .equals(applicationList.getUuid()))
                                    .findFirst()
                                    .orElseThrow();

                    return new UUID[] {applicationList.getUuid(), applicationListEntry.getUuid()};
                });
    }

    private EntryUpdateDto validEntryUpdateDto() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryUpdateDto updateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();

        updateDto.getApplicant().setPerson(null);
        updateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA13 1BB");
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@org.com"));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));

        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA12 1AA");
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@test.com"));
        updateDto.getRespondent().getPerson().getName().setMiddleName(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setPhone(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setMobile(JsonNullable.of(null));

        updateDto.getRespondent().setOrganisation(null);
        updateDto.setStandardApplicantCode(null);
        updateDto.setOfficials(CreateEntryDtoUtil.validOfficials());
        updateDto.setApplicationCode("ZS99007");
        updateDto.setHasOffsiteFee(true);
        updateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Premises Address", "test wording"),
                        new TemplateSubstitution(
                                "Premises Date",
                                LocalDate.now(java.time.ZoneOffset.UTC).toString())));
        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(updateDto.getFeeStatuses());

        return updateDto;
    }
}
