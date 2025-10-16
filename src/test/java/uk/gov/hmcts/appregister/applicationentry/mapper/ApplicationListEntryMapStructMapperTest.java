package uk.gov.hmcts.appregister.applicationentry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationListEntryMapStructMapperTest {

    @Test
    void testToSummaryModel_provideValidData_validModelGenerated() {
        var sequenceNumber = 1;
        var accountNumber = "1234567890";
        var applicant = "Mustafa's Org";
        var respondent = "Ahmed, Mustafa, His Majesty";
        var postCode = "SW1A 1AA";
        var applicationTitle = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired = true;
        var result = "APPC";

        var projection = new ProjectionStub()
            .withSequenceNumber((short) sequenceNumber)
            .withAccountNumber(accountNumber)
            .withApplicant(applicant)
            .withRespondent(respondent)
            .withPostCode(postCode)
            .withApplicationTitle(applicationTitle)
            .withFeeRequired(feeRequired)
            .withResult(result);

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        var model = mapper.toSummaryModel(projection);

        Assertions.assertEquals(sequenceNumber, model.getSequenceNumber());
        Assertions.assertEquals(accountNumber, model.getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant, model.getApplicant().orElse(null));
        Assertions.assertEquals(respondent, model.getRespondent().orElse(null));
        Assertions.assertEquals(postCode, model.getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle, model.getApplicationTitle());
        Assertions.assertEquals(feeRequired, model.getFeeRequired());
        Assertions.assertEquals(result, model.getResult().orElse(null));
    }

    @Test
    void testToSummaryModelList_provideValidData_validModelListGenerated() {
        var sequenceNumber1 = 1;
        var accountNumber1 = "1234567890";
        var applicant1 = "Mustafa's Org";
        var respondent1 = "Ahmed, Mustafa, His Majesty";
        var postCode1 = "SW1A 1AA";
        var applicationTitle1 = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired1 = true;
        var result1 = "APPC";

        var sequenceNumber2 = 2;
        var accountNumber2 = "1234567891";
        var applicant2 = "AW62958 300919";
        var respondent2 = "Johnson, Sarah";
        var postCode2 = "EH1 3QR";
        var applicationTitle2 = "Copy documents";
        var feeRequired2 = false;
        var result2 = "RESP";

        var projection1 = new ProjectionStub()
            .withSequenceNumber((short) sequenceNumber1)
            .withAccountNumber(accountNumber1)
            .withApplicant(applicant1)
            .withRespondent(respondent1)
            .withPostCode(postCode1)
            .withApplicationTitle(applicationTitle1)
            .withFeeRequired(feeRequired1)
            .withResult(result1);

        var projection2 = new ProjectionStub()
            .withSequenceNumber((short) sequenceNumber2)
            .withAccountNumber(accountNumber2)
            .withApplicant(applicant2)
            .withRespondent(respondent2)
            .withPostCode(postCode2)
            .withApplicationTitle(applicationTitle2)
            .withFeeRequired(feeRequired2)
            .withResult(result2);

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        List<ApplicationListEntrySummary> list =
            mapper.toSummaryModelList(List.of(projection1, projection2));

        assertThat(list).hasSize(2);

        Assertions.assertEquals(sequenceNumber1, list.getFirst().getSequenceNumber());
        Assertions.assertEquals(accountNumber1, list.getFirst().getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant1, list.getFirst().getApplicant().orElse(null));
        Assertions.assertEquals(respondent1, list.getFirst().getRespondent().orElse(null));
        Assertions.assertEquals(postCode1, list.getFirst().getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle1, list.getFirst().getApplicationTitle());
        Assertions.assertEquals(feeRequired1, list.getFirst().getFeeRequired());
        Assertions.assertEquals(result1, list.getFirst().getResult().orElse(null));

        Assertions.assertEquals(sequenceNumber2, list.getLast().getSequenceNumber());
        Assertions.assertEquals(accountNumber2, list.getLast().getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant2, list.getLast().getApplicant().orElse(null));
        Assertions.assertEquals(respondent2, list.getLast().getRespondent().orElse(null));
        Assertions.assertEquals(postCode2, list.getLast().getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle2, list.getLast().getApplicationTitle());
        Assertions.assertEquals(feeRequired2, list.getLast().getFeeRequired());
        Assertions.assertEquals(result2, list.getLast().getResult().orElse(null));
    }

    /**
     * Minimal test double of the projection interface.
     */
    private static class ProjectionStub implements ApplicationListEntrySummaryProjection {
        private short sequenceNumber;
        private String accountNumber;
        private String applicant;
        private String respondent;
        private String postCode;
        private String applicationTitle;
        private boolean feeRequired;
        private String result;

        ProjectionStub withSequenceNumber(short sequenceNumber) { this.sequenceNumber = sequenceNumber; return this; }
        ProjectionStub withAccountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        ProjectionStub withApplicant(String applicant) { this.applicant = applicant; return this; }
        ProjectionStub withRespondent(String respondent) { this.respondent = respondent; return this; }
        ProjectionStub withPostCode(String postCode) { this.postCode = postCode; return this; }
        ProjectionStub withApplicationTitle(String applicationTitle) {
            this.applicationTitle = applicationTitle; return this; }
        ProjectionStub withFeeRequired(boolean feeRequired) { this.feeRequired = feeRequired; return this; }
        ProjectionStub withResult(String result) { this.result = result; return this; }

        // --- Implement projection getters ---
        @Override public short getSequenceNumber() { return sequenceNumber; }
        @Override public String getAccountNumber() { return accountNumber; }
        @Override public String getApplicant() { return applicant; }
        @Override public String getRespondent() { return respondent; }
        @Override public String getPostCode() { return postCode; }
        @Override public String getApplicationTitle() { return applicationTitle; }
        @Override public boolean isFeeRequired() { return feeRequired; }
        @Override public String getResult() { return result; }
    }
}
