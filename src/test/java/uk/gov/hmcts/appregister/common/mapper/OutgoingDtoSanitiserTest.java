package uk.gov.hmcts.appregister.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;

class OutgoingDtoSanitiserTest {

    @Test
    void emptyToNull_convertsBlankStringAndJsonNullable() {
        assertThat(OutgoingDtoSanitiser.emptyToNull("")).isNull();
        assertThat(OutgoingDtoSanitiser.emptyToNull("value")).isEqualTo("value");

        var blankNullable = OutgoingDtoSanitiser.emptyToNull(JsonNullable.of(""));
        assertThat(blankNullable.isPresent()).isTrue();
        assertThat(blankNullable.orElse("fallback")).isNull();

        assertThat(OutgoingDtoSanitiser.emptyToNull(JsonNullable.of("value")).orElse("fallback"))
                .isEqualTo("value");
    }

    @Test
    void sanitizeApplicationListGetPrintDto_convertsBlankStringsAndHandlesNullEntries() {
        var dto =
                new ApplicationListGetPrintDto()
                        .date(LocalDate.of(2026, Month.JUNE, 23))
                        .time(LocalTime.NOON)
                        .courtName("")
                        .cja("")
                        .otherLocationDescription("")
                        .duration("")
                        .entries(null);

        var sanitized = OutgoingDtoSanitiser.sanitize(dto);

        assertThat(sanitized.getCourtName()).isNull();
        assertThat(sanitized.getCja()).isNull();
        assertThat(sanitized.getOtherLocationDescription()).isNull();
        assertThat(sanitized.getDuration()).isNull();
        assertThat(sanitized.getEntries()).isNull();
    }

    @Test
    void sanitizeApplicationListGetPrintDto_sanitizesNestedEntries() {
        var entry = new EntryGetPrintDto().applicationTitle("").accountReference("").notes("");
        var dto =
                new ApplicationListGetPrintDto()
                        .date(LocalDate.of(2026, Month.JUNE, 23))
                        .time(LocalTime.NOON)
                        .entries(List.of(entry));

        var sanitized = OutgoingDtoSanitiser.sanitize(dto);

        assertThat(sanitized.getEntries())
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.getApplicationTitle()).isNull();
                            assertThat(item.getAccountReference()).isNull();
                            assertThat(item.getNotes()).isNull();
                        });
    }

    @Test
    void sanitizeApplicationCodeDtos_convertsBlankFieldsToNull() {
        var summary =
                new ApplicationCodeGetSummaryDto()
                        .applicationCode("")
                        .title("")
                        .feeReference("")
                        .feeDescription("")
                        .offsiteFeeReference("")
                        .offsiteFeeDescription("")
                        .wording(
                                new TemplateDetail()
                                        .template("")
                                        .substitutionKeyConstraints(
                                                List.of(
                                                        new TemplateKeyWithConstraint()
                                                                .key("")
                                                                .value(""))));

        var detail =
                new ApplicationCodeGetDetailDto()
                        .applicationCode("")
                        .title("")
                        .feeReference("")
                        .feeDescription("")
                        .offsiteFeeReference("")
                        .offsiteFeeDescription("")
                        .wording(new TemplateDetail().template(""));

        var sanitizedSummary = OutgoingDtoSanitiser.sanitize(summary);
        var sanitizedDetail = OutgoingDtoSanitiser.sanitize(detail);

        assertThat(sanitizedSummary.getApplicationCode()).isNull();
        assertThat(sanitizedSummary.getTitle()).isNull();
        assertThat(sanitizedSummary.getFeeReference().orElse("value")).isNull();
        assertThat(sanitizedSummary.getFeeDescription().orElse("value")).isNull();
        assertThat(sanitizedSummary.getOffsiteFeeReference().orElse("value")).isNull();
        assertThat(sanitizedSummary.getOffsiteFeeDescription().orElse("value")).isNull();
        assertThat(sanitizedSummary.getWording().getTemplate()).isNull();
        assertThat(sanitizedSummary.getWording().getSubstitutionKeyConstraints())
                .singleElement()
                .satisfies(
                        constraint -> {
                            assertThat(constraint.getKey()).isNull();
                            assertThat(constraint.getValue()).isNull();
                        });

        assertThat(sanitizedDetail.getApplicationCode()).isNull();
        assertThat(sanitizedDetail.getTitle()).isNull();
        assertThat(sanitizedDetail.getFeeReference().orElse("value")).isNull();
        assertThat(sanitizedDetail.getFeeDescription().orElse("value")).isNull();
        assertThat(sanitizedDetail.getOffsiteFeeReference().orElse("value")).isNull();
        assertThat(sanitizedDetail.getOffsiteFeeDescription().orElse("value")).isNull();
        assertThat(sanitizedDetail.getWording().getTemplate()).isNull();
    }

    @Test
    void sanitizeResultAndLocationDtos_convertsBlankFieldsToNull() {
        var resultSummary = new ResultCodeGetSummaryDto().resultCode("").title("");
        var resultDetail =
                new ResultCodeGetDetailDto()
                        .resultCode("")
                        .title("")
                        .wording(new TemplateDetail().template(""));
        var courtSummary = new CourtLocationGetSummaryDto().name("").locationCode("");
        var courtDetail = new CourtLocationGetDetailDto().name("").locationCode("");
        var cja = new CriminalJusticeAreaGetDto().code("").description("");

        var sanitizedResultSummary = OutgoingDtoSanitiser.sanitize(resultSummary);
        assertThat(sanitizedResultSummary.getResultCode()).isNull();
        assertThat(sanitizedResultSummary.getTitle()).isNull();

        var sanitizedResultDetail = OutgoingDtoSanitiser.sanitize(resultDetail);
        assertThat(sanitizedResultDetail.getResultCode()).isNull();
        assertThat(sanitizedResultDetail.getTitle()).isNull();
        assertThat(sanitizedResultDetail.getWording().getTemplate()).isNull();

        var sanitizedCourtSummary = OutgoingDtoSanitiser.sanitize(courtSummary);
        assertThat(sanitizedCourtSummary.getName()).isNull();
        assertThat(sanitizedCourtSummary.getLocationCode()).isNull();

        var sanitizedCourtDetail = OutgoingDtoSanitiser.sanitize(courtDetail);
        assertThat(sanitizedCourtDetail.getName()).isNull();
        assertThat(sanitizedCourtDetail.getLocationCode()).isNull();

        var sanitizedCja = OutgoingDtoSanitiser.sanitize(cja);
        assertThat(sanitizedCja.getCode()).isNull();
        assertThat(sanitizedCja.getDescription()).isNull();
    }
}
