package uk.gov.hmcts.appregister.applicationentry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionUtil.applicationListEntrySummaryProjection;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;

class ApplicationListEntryMapStructMapperTest {

    @Test
    void testToSummaryModel_provideValidData_validModelGenerated() {
        var uuid = UUID.randomUUID();
        var sequenceNumber = 1;
        var accountNumber = "1234567890";
        var applicant = "Mustafa's Org";
        var respondent = "Ahmed, Mustafa, His Majesty";
        var postCode = "SW1A 1AA";
        var applicationTitle = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired = true;
        var result = "APPC";
        var projection =
                applicationListEntrySummaryProjection()
                        .uuid(uuid)
                        .sequenceNumber(sequenceNumber)
                        .accountNumber(accountNumber)
                        .applicant(applicant)
                        .respondent(respondent)
                        .postCode(postCode)
                        .applicationTitle(applicationTitle)
                        .feeRequired(feeRequired)
                        .result(result)
                        .build();

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        var model = mapper.toSummaryDto(projection);

        assertApplicationListEntrySummary(
                uuid,
                sequenceNumber,
                model,
                accountNumber,
                applicant,
                respondent,
                postCode,
                applicationTitle,
                feeRequired,
                result);
    }

    @Test
    void testToSummaryModelList_provideValidData_validModelListGenerated() {
        var uuid1 = UUID.randomUUID();
        var sequenceNumber1 = 1;
        var accountNumber1 = "1234567890";
        var applicant1 = "Mustafa's Org";
        var respondent1 = "Ahmed, Mustafa, His Majesty";
        var postCode1 = "SW1A 1AA";
        var applicationTitle1 = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired1 = true;
        var result1 = "APPC";
        var projection1 =
                applicationListEntrySummaryProjection()
                        .uuid(uuid1)
                        .sequenceNumber(sequenceNumber1)
                        .accountNumber(accountNumber1)
                        .applicant(applicant1)
                        .respondent(respondent1)
                        .postCode(postCode1)
                        .applicationTitle(applicationTitle1)
                        .feeRequired(feeRequired1)
                        .result(result1)
                        .build();

        var uuid2 = UUID.randomUUID();
        var sequenceNumber2 = 2;
        var accountNumber2 = "1234567891";
        var applicant2 = "AW62958 300919";
        var respondent2 = "Johnson, Sarah";
        var postCode2 = "EH1 3QR";
        var applicationTitle2 = "Copy documents";
        var feeRequired2 = false;
        var result2 = "RESP";
        var projection2 =
                applicationListEntrySummaryProjection()
                        .uuid(uuid2)
                        .sequenceNumber(sequenceNumber2)
                        .accountNumber(accountNumber2)
                        .applicant(applicant2)
                        .respondent(respondent2)
                        .postCode(postCode2)
                        .applicationTitle(applicationTitle2)
                        .feeRequired(feeRequired2)
                        .result(result2)
                        .build();

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        List<ApplicationListEntrySummary> list =
                mapper.toSummaryDtoList(List.of(projection1, projection2));

        assertThat(list).hasSize(2);

        assertApplicationListEntrySummary(
                uuid1,
                sequenceNumber1,
                list.getFirst(),
                accountNumber1,
                applicant1,
                respondent1,
                postCode1,
                applicationTitle1,
                feeRequired1,
                result1);

        assertApplicationListEntrySummary(
                uuid2,
                sequenceNumber2,
                list.getLast(),
                accountNumber2,
                applicant2,
                respondent2,
                postCode2,
                applicationTitle2,
                feeRequired2,
                result2);
    }

    private static void assertApplicationListEntrySummary(
            UUID uuid,
            int sequenceNumber,
            ApplicationListEntrySummary dto,
            String accountNumber,
            String applicant,
            String respondent,
            String postCode,
            String applicationTitle,
            boolean feeRequired,
            String result) {
        Assertions.assertEquals(uuid, dto.getUuid());
        Assertions.assertEquals(sequenceNumber, dto.getSequenceNumber());
        Assertions.assertEquals(accountNumber, dto.getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant, dto.getApplicant().orElse(null));
        Assertions.assertEquals(respondent, dto.getRespondent().orElse(null));
        Assertions.assertEquals(postCode, dto.getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle, dto.getApplicationTitle());
        Assertions.assertEquals(feeRequired, dto.getFeeRequired());
        Assertions.assertEquals(result, dto.getResult().orElse(null));
    }
}
