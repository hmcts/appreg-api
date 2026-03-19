package uk.gov.hmcts.appregister.testutils.util;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapperImpl;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * A useful assertion class for the create/update application entry functionality. This class aims
 * to handle the bulk of the core validation
 *
 * <p>This class takes the request and response of the operation as well as the resulting database
 * entity. From this information it can assert what the database entity should look like as well as
 * the response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationListEntryAssertion {
    private final AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    private final ApplicationListEntryOfficialRepository applicationListEntryOfficialRepository;

    private final FeeRepository feeRepository;

    /**
     * validates the database and the restful output based on the input payload.
     *
     * @param entryCreateDto The entry create dto
     * @param applicationListEntry The application list database entity that was created
     * @param response The response from the service
     * @param assertWordingApplied The wording with applied values
     * @param assertWording The wording we expect to result from the associated code wording
     *     template
     * @param expectedWordingFields The fields associated with the wording template that we expect
     *     in the response
     */
    @Transactional
    public void validateEntityAndResponseForEntryCreation(
            ApplicationListEntryWrapperDto entryCreateDto,
            ApplicationListEntry applicationListEntry,
            EntryGetDetailDto response,
            String assertWordingApplied,
            String assertWording,
            List<TemplateSubstitution> expectedWordingFields) {
        validateEntityAndResponseForEntryUpdate(
                entryCreateDto,
                applicationListEntry,
                response,
                assertWordingApplied,
                assertWording,
                expectedWordingFields,
                List.of());
    }

    /**
     * validates the database and the restful output based on the input dto payload.
     *
     * @param entryCreateUpdateDto The entry create dto
     * @param applicationListEntry The application list database entity that was created
     * @param response The response from the service
     * @param assertWording The wording we expect to result from the associated code wording
     *     template
     * @param expectedWordingFields The fields associated with the wording template that we expect
     *     in the response
     * @param existingFeeStatuses When updating these are the fee statuses that already exist prior
     *     to the update operation taking place. This is used to offset the validation of the api
     *     response and database records
     */
    @Transactional
    public void validateEntityAndResponseForEntryUpdate(
            ApplicationListEntryWrapperDto entryCreateUpdateDto,
            ApplicationListEntry applicationListEntry,
            EntryGetDetailDto response,
            String assertWordingApplied,
            String assertWording,
            List<TemplateSubstitution> expectedWordingFields,
            List<Long> existingFeeStatuses) {

        // validate applicant with the dto
        if (entryCreateUpdateDto.getStandardApplicantCode() != null) {
            // make sure the list is mapped to the code
            Assertions.assertEquals(
                    entryCreateUpdateDto.getStandardApplicantCode(),
                    applicationListEntry.getStandardApplicant().getApplicantCode());
        } else {
            if (entryCreateUpdateDto.getApplicant().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        entryCreateUpdateDto.getApplicant().getPerson(),
                        applicationListEntry.getAnamedaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        entryCreateUpdateDto.getApplicant().getOrganisation(),
                        applicationListEntry.getAnamedaddress());
            }
        }

        // validate respondent with the dto
        if (entryCreateUpdateDto.getRespondent() != null) {
            if (entryCreateUpdateDto.getRespondent().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        entryCreateUpdateDto.getRespondent().getPerson(),
                        applicationListEntry.getRnameaddress());

                Assertions.assertEquals(
                        entryCreateUpdateDto.getRespondent().getPerson().getDateOfBirth(),
                        applicationListEntry.getRnameaddress().getDateOfBirth());
            } else {
                ApplicantAssertion.validateOrganisation(
                        entryCreateUpdateDto.getRespondent().getOrganisation(),
                        applicationListEntry.getRnameaddress());
            }
        }

        // make sure the code of the applicant and respondent are set correctly in the database
        if (entryCreateUpdateDto.getApplicant() != null
                && (entryCreateUpdateDto.getApplicant().getPerson() != null
                        || entryCreateUpdateDto.getApplicant().getOrganisation() != null)) {
            // validate the application code
            Assertions.assertEquals(
                    NameAddressCodeType.APPLICANT,
                    applicationListEntry.getAnamedaddress().getCode());
        } else {
            Assertions.assertNull(applicationListEntry.getAnamedaddress());
            Assertions.assertEquals(
                    entryCreateUpdateDto.getStandardApplicantCode(),
                    applicationListEntry.getStandardApplicant().getApplicantCode());
        }

        if (entryCreateUpdateDto.getRespondent() != null
                && (entryCreateUpdateDto.getRespondent().getPerson() != null
                        || entryCreateUpdateDto.getRespondent().getOrganisation() != null)) {
            // validate the application code
            Assertions.assertEquals(
                    NameAddressCodeType.RESPONDENT,
                    applicationListEntry.getRnameaddress().getCode());
        }

        // if number or respondents is set make sure it was saved
        if (entryCreateUpdateDto.getNumberOfRespondents() != null) {
            Assertions.assertEquals(
                    entryCreateUpdateDto.getNumberOfRespondents(),
                    applicationListEntry.getNumberOfBulkRespondents().intValue());
        }

        // make sure the core data is part of the entry
        Assertions.assertEquals(
                entryCreateUpdateDto.getCaseReference(), applicationListEntry.getCaseReference());
        Assertions.assertEquals(entryCreateUpdateDto.getNotes(), applicationListEntry.getNotes());
        Assertions.assertEquals(
                entryCreateUpdateDto.getAccountNumber(), applicationListEntry.getAccountNumber());
        Assertions.assertEquals(
                entryCreateUpdateDto.getLodgementDate(), applicationListEntry.getLodgementDate());

        // assert that the wording template in the code has been processed correctly
        Assertions.assertEquals(
                assertWordingApplied, applicationListEntry.getApplicationListEntryWording());

        // validate the fees are created in the database and they are aligned with offsite
        List<AppListEntryFeeStatus> fees =
                appListEntryFeeStatusRepository.findByAppListEntryId(applicationListEntry.getId());
        // assert offsite fee in the database is set if provided
        if (entryCreateUpdateDto.getFeeStatuses() != null
                && !entryCreateUpdateDto.getFeeStatuses().isEmpty()) {
            Assertions.assertFalse(applicationListEntry.getEntryFeeIds().isEmpty());

            boolean containsOffsite = false;
            for (AppListEntryFeeId fee : applicationListEntry.getEntryFeeIds()) {
                containsOffsite = feeRepository.findById(fee.getFeeId()).get().isOffsite();
                if (containsOffsite) {
                    break;
                }
            }
            Assertions.assertEquals(entryCreateUpdateDto.getHasOffsiteFee(), containsOffsite);
        }

        // ensure the database fees align ignoring pre existing fee statuses
        if (entryCreateUpdateDto.getFeeStatuses() != null) {
            for (int i = 0; i < entryCreateUpdateDto.getFeeStatuses().size(); i++) {
                if (i >= existingFeeStatuses.size()) {

                    // find the actual fee status in the database
                    AppListEntryFeeStatus actualFeeStatus =
                            getFeeForReference(
                                    fees,
                                    entryCreateUpdateDto
                                            .getFeeStatuses()
                                            .get(i - existingFeeStatuses.size())
                                            .getPaymentReference());

                    Assertions.assertNotNull(actualFeeStatus);
                    Assertions.assertEquals(
                            entryCreateUpdateDto
                                    .getFeeStatuses()
                                    .get(i - existingFeeStatuses.size())
                                    .getPaymentReference(),
                            actualFeeStatus.getAlefsPaymentReference());

                    Assertions.assertEquals(
                            ApplicationListEntryEntityMapper.toStatus(
                                    entryCreateUpdateDto
                                            .getFeeStatuses()
                                            .get(i - existingFeeStatuses.size())
                                            .getPaymentStatus()),
                            actualFeeStatus.getAlefsFeeStatus());
                }
            }
        }

        // validate the original are created
        List<AppListEntryOfficial> originals =
                applicationListEntryOfficialRepository.findByAppListEntryId(
                        applicationListEntry.getId());

        if (entryCreateUpdateDto.getOfficials() != null) {
            for (int i = 0; i < entryCreateUpdateDto.getOfficials().size(); i++) {
                Assertions.assertEquals(
                        entryCreateUpdateDto.getOfficials().get(i).getTitle(),
                        originals.get(i).getTitle());
                Assertions.assertEquals(
                        entryCreateUpdateDto.getOfficials().get(i).getForename(),
                        originals.get(i).getForename());
                Assertions.assertEquals(
                        entryCreateUpdateDto.getOfficials().get(i).getSurname(),
                        originals.get(i).getSurname());
                Assertions.assertEquals(
                        new OfficialMapperImpl()
                                .toOfficial(entryCreateUpdateDto.getOfficials().get(i).getType()),
                        originals.get(i).getOfficialType());
            }
        }

        // validate applicant in the response
        if (entryCreateUpdateDto.getStandardApplicantCode() != null) {
            // determine how the applicant should be represented
            Applicant applicant =
                    new ApplicantMapperImpl()
                            .toApplicant(
                                    new ApplicantMapperImpl()
                                            .toApplicantEntity(
                                                    applicationListEntry.getStandardApplicant()));

            // validate the SA person or organisation
            if (applicant.getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getApplicant().getPerson(),
                        applicationListEntry.getStandardApplicant());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getApplicant().getOrganisation(),
                        applicationListEntry.getStandardApplicant());
            }
        } else {
            // if not a SA assert the expected response based on the input
            if (entryCreateUpdateDto.getApplicant().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getApplicant().getPerson(),
                        applicationListEntry.getAnamedaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getApplicant().getOrganisation(),
                        applicationListEntry.getAnamedaddress());
            }
        }

        // validate respondent
        if (entryCreateUpdateDto.getRespondent() != null) {
            if (entryCreateUpdateDto.getRespondent().getPerson() != null) {
                ApplicantAssertion.validatePerson(
                        response.getRespondent().getPerson(),
                        applicationListEntry.getRnameaddress());
            } else {
                ApplicantAssertion.validateOrganisation(
                        response.getRespondent().getOrganisation(),
                        applicationListEntry.getRnameaddress());
            }
        }

        // validate the response fields
        Assertions.assertEquals(
                entryCreateUpdateDto.getCaseReference(), response.getCaseReference());
        Assertions.assertEquals(entryCreateUpdateDto.getNotes(), response.getNotes());
        Assertions.assertEquals(
                entryCreateUpdateDto.getAccountNumber(), response.getAccountNumber());

        // lets assert that the response should contain the wording template fields
        TemplateAssertion.assertTemplateWithValues(
                assertWording, expectedWordingFields, response.getWording());

        Assertions.assertEquals(
                applicationListEntry.getApplicationList().getUuid(), response.getListId());
        Assertions.assertEquals(applicationListEntry.getUuid(), response.getId());
        Assertions.assertEquals(
                entryCreateUpdateDto.getNumberOfRespondents(), response.getNumberOfRespondents());
        Assertions.assertEquals(
                entryCreateUpdateDto.getLodgementDate(), response.getLodgementDate());
        Assertions.assertEquals(
                entryCreateUpdateDto.getHasOffsiteFee(), response.getHasOffsiteFee());

        if (entryCreateUpdateDto.getFeeStatuses() != null) {
            // assert that we are returning all fee statuses including pre existing ones
            Assertions.assertEquals(
                    response.getFeeStatuses().size(),
                    entryCreateUpdateDto.getFeeStatuses().size() + existingFeeStatuses.size());
        }

        // ensure the response fees align ignoring pre existing fee statuses in the response
        for (int i = 0; i < response.getFeeStatuses().size(); i++) {
            // lets ignore the existing fee statuses and assert against the new ones
            if (i >= existingFeeStatuses.size()) {
                Assertions.assertEquals(
                        entryCreateUpdateDto
                                .getFeeStatuses()
                                .get(i - existingFeeStatuses.size())
                                .getPaymentReference(),
                        response.getFeeStatuses().get(i).getPaymentReference());
                Assertions.assertEquals(
                        entryCreateUpdateDto
                                .getFeeStatuses()
                                .get(i - existingFeeStatuses.size())
                                .getStatusDate(),
                        response.getFeeStatuses().get(i).getStatusDate());
                Assertions.assertEquals(
                        entryCreateUpdateDto
                                .getFeeStatuses()
                                .get(i - existingFeeStatuses.size())
                                .getPaymentStatus(),
                        response.getFeeStatuses().get(i).getPaymentStatus());
            }
        }

        // ensure the responses aligfns with the officials specified
        for (int i = 0; i < response.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    entryCreateUpdateDto.getOfficials().get(i).getType(),
                    response.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    entryCreateUpdateDto.getOfficials().get(i).getSurname(),
                    response.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    entryCreateUpdateDto.getOfficials().get(i).getTitle(),
                    response.getOfficials().get(i).getTitle());
            Assertions.assertEquals(
                    entryCreateUpdateDto.getOfficials().get(i).getForename(),
                    response.getOfficials().get(i).getForename());
        }
    }

    private AppListEntryFeeStatus getFeeForReference(
            List<AppListEntryFeeStatus> statusLst, String ref) {
        for (AppListEntryFeeStatus appListEntryFeeStatus : statusLst) {
            if (Objects.equals(appListEntryFeeStatus.getAlefsPaymentReference(), ref)) {
                return appListEntryFeeStatus;
            }
        }
        return null;
    }
}
