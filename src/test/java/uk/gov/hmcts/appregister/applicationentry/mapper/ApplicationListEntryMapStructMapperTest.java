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

        var projection = createProjectionStub(sequenceNumber, accountNumber, applicant, respondent, postCode,
                                              applicationTitle, feeRequired, result);

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        var model = mapper.toSummaryModel(projection);

        assertApplicationListEntrySummary(sequenceNumber, model, accountNumber, applicant, respondent, postCode,
            applicationTitle, feeRequired, result
        );
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

        var projection1 = createProjectionStub(sequenceNumber1, accountNumber1, applicant1, respondent1, postCode1,
                                              applicationTitle1, feeRequired1, result1);

        var sequenceNumber2 = 2;
        var accountNumber2 = "1234567891";
        var applicant2 = "AW62958 300919";
        var respondent2 = "Johnson, Sarah";
        var postCode2 = "EH1 3QR";
        var applicationTitle2 = "Copy documents";
        var feeRequired2 = false;
        var result2 = "RESP";

        var projection2 = createProjectionStub(sequenceNumber2, accountNumber2, applicant2, respondent2, postCode2,
                                               applicationTitle2, feeRequired2, result2);

        var mapper = new ApplicationListEntryMapStructMapperImpl();
        List<ApplicationListEntrySummary> list =
            mapper.toSummaryModelList(List.of(projection1, projection2));

        assertThat(list).hasSize(2);

        assertApplicationListEntrySummary(sequenceNumber1, list.getFirst(), accountNumber1, applicant1, respondent1,
                                          postCode1, applicationTitle1, feeRequired1, result1
        );

        assertApplicationListEntrySummary(sequenceNumber2, list.getLast(), accountNumber2, applicant2, respondent2,
                                          postCode2, applicationTitle2, feeRequired2, result2
        );
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

    private ProjectionStub createProjectionStub(
        int sequenceNumber,
        String accountNumber,
        String applicant,
        String respondent,
        String postCode,
        String applicationTitle,
        boolean feeRequired,
        String result) {

        return new ProjectionStub()
            .withSequenceNumber((short) sequenceNumber)
            .withAccountNumber(accountNumber)
            .withApplicant(applicant)
            .withRespondent(respondent)
            .withPostCode(postCode)
            .withApplicationTitle(applicationTitle)
            .withFeeRequired(feeRequired)
            .withResult(result);
    }

    private static void assertApplicationListEntrySummary(int sequenceNumber, ApplicationListEntrySummary model,
                                                          String accountNumber, String applicant, String respondent,
                                                          String postCode, String applicationTitle, boolean feeRequired,
                                                          String result) {
        Assertions.assertEquals(sequenceNumber, model.getSequenceNumber());
        Assertions.assertEquals(accountNumber, model.getAccountNumber().orElse(null));
        Assertions.assertEquals(applicant, model.getApplicant().orElse(null));
        Assertions.assertEquals(respondent, model.getRespondent().orElse(null));
        Assertions.assertEquals(postCode, model.getPostCode().orElse(null));
        Assertions.assertEquals(applicationTitle, model.getApplicationTitle());
        Assertions.assertEquals(feeRequired, model.getFeeRequired());
        Assertions.assertEquals(result, model.getResult().orElse(null));
    }
}
