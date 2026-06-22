package uk.gov.hmcts.appregister.applicationcode.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapperImpl;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoOffsiteFeeAmount;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;

class ApplicationCodeMapperTest {
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, Month.JANUARY, 2);

    private final ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapperImpl();

    @Test
    void testWithCompleteMapApplicationCodeGetSummaryDto() {
        Fee fee = new Fee();
        fee.setAmount(BigDecimal.valueOf(232.34));
        fee.setDescription("Description");
        fee.setOffsite(false);
        fee.setReference("reference");

        Fee offsitefee = new Fee();
        offsitefee.setAmount(BigDecimal.valueOf(23666.34));
        offsitefee.setDescription("Description offset");
        offsitefee.setOffsite(true);
        offsitefee.setReference("offsite fee");

        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(EFFECTIVE_DATE);
        code.setStartDate(EFFECTIVE_DATE);

        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());
        ApplicationCodeGetSummaryDto summaryDto =
                applicationCodeMapper.toApplicationCodeGetSummaryDto(code, fee, offsitefee);

        // assert
        Assertions.assertEquals(
                "Specify Document Lost",
                summaryDto.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                100,
                summaryDto
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getLength());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                summaryDto
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getType());

        Assertions.assertEquals(
                "namely {{Specify Document Lost}}", summaryDto.getWording().getTemplate());
        Assertions.assertEquals(
                "Specify Document Lost",
                summaryDto.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                100,
                summaryDto
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getLength());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                summaryDto
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getType());

        Assertions.assertEquals("appcode", summaryDto.getApplicationCode());
        Assertions.assertEquals(23234L, summaryDto.getFeeAmount().get().getValue());
        Assertions.assertEquals(
                ApplicationCodeGetSummaryDtoFeeAmount.CurrencyEnum.GBP,
                summaryDto.getFeeAmount().get().getCurrency());
        Assertions.assertEquals(2366634, summaryDto.getOffsiteFeeAmount().get().getValue());
        Assertions.assertEquals(
                ApplicationCodeGetSummaryDtoOffsiteFeeAmount.CurrencyEnum.GBP,
                summaryDto.getOffsiteFeeAmount().get().getCurrency());

        Assertions.assertEquals("reference", summaryDto.getFeeReference().get());
        Assertions.assertEquals("offsite fee", summaryDto.getOffsiteFeeReference().get());

        Assertions.assertEquals(Boolean.FALSE, summaryDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, summaryDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, summaryDto.getBulkRespondentAllowed());
        Assertions.assertEquals("Description", summaryDto.getFeeDescription().get());
        Assertions.assertEquals("Description offset", summaryDto.getOffsiteFeeDescription().get());
    }

    @Test
    void testWithoutFeesMapApplicationCodeGetSummaryDto() {
        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(EFFECTIVE_DATE);
        code.setStartDate(EFFECTIVE_DATE);
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());
        ApplicationCodeGetSummaryDto summaryDto =
                applicationCodeMapper.toApplicationCodeGetSummaryDto(code, null, null);

        // assert
        Assertions.assertEquals("appcode", summaryDto.getApplicationCode());
        Assertions.assertFalse(summaryDto.getFeeAmount().isPresent());
        Assertions.assertFalse(summaryDto.getOffsiteFeeAmount().isPresent());
        Assertions.assertEquals(Boolean.FALSE, summaryDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, summaryDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, summaryDto.getBulkRespondentAllowed());
        Assertions.assertFalse(summaryDto.getFeeReference().isPresent());
        Assertions.assertFalse(summaryDto.getFeeDescription().isPresent());
    }

    @Test
    void testWithCompleteMapApplicationCodeGetDetailDto() {
        Fee fee = new Fee();
        fee.setAmount(BigDecimal.valueOf(232.34));
        fee.setDescription("Description");
        fee.setOffsite(false);
        fee.setReference("reference");

        Fee offsetfee = new Fee();
        offsetfee.setAmount(BigDecimal.valueOf(23666.34));
        offsetfee.setDescription("Description offset");
        offsetfee.setOffsite(true);
        offsetfee.setReference("offsite fee");

        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(EFFECTIVE_DATE);
        code.setStartDate(EFFECTIVE_DATE);

        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());
        ApplicationCodeGetDetailDto getDetailDto =
                applicationCodeMapper.toApplicationCodeGetDetailDto(code, fee, offsetfee);

        // assert
        Assertions.assertEquals("appcode", getDetailDto.getApplicationCode());
        Assertions.assertEquals(23234L, getDetailDto.getFeeAmount().get().getValue());
        Assertions.assertEquals(
                ApplicationCodeGetSummaryDtoFeeAmount.CurrencyEnum.GBP,
                getDetailDto.getFeeAmount().get().getCurrency());
        Assertions.assertEquals(2366634, getDetailDto.getOffsiteFeeAmount().get().getValue());
        Assertions.assertEquals(
                ApplicationCodeGetSummaryDtoOffsiteFeeAmount.CurrencyEnum.GBP,
                getDetailDto.getOffsiteFeeAmount().get().getCurrency());
        Assertions.assertEquals("reference", getDetailDto.getFeeReference().get());
        Assertions.assertEquals("offsite fee", getDetailDto.getOffsiteFeeReference().get());
        Assertions.assertEquals(
                "Description offset", getDetailDto.getOffsiteFeeDescription().get());

        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, getDetailDto.getBulkRespondentAllowed());
        Assertions.assertEquals("Description", getDetailDto.getFeeDescription().get());
    }

    @Test
    void testWithoutFeesMapApplicationCodeGetDetailDto() {
        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(EFFECTIVE_DATE);
        code.setStartDate(EFFECTIVE_DATE);
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());
        ApplicationCodeGetDetailDto getDetailDto =
                applicationCodeMapper.toApplicationCodeGetDetailDto(code, null, null);

        // assert
        Assertions.assertEquals("appcode", getDetailDto.getApplicationCode());
        Assertions.assertFalse(getDetailDto.getFeeAmount().isPresent());
        Assertions.assertFalse(getDetailDto.getOffsiteFeeAmount().isPresent());
        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, getDetailDto.getBulkRespondentAllowed());
        Assertions.assertFalse(getDetailDto.getFeeReference().isPresent());
        Assertions.assertFalse(getDetailDto.getFeeDescription().isPresent());
    }

    @Test
    void feeWithMoreThan2dpThrows() {
        Fee fee = new Fee();
        fee.setAmount(new BigDecimal("10.005")); // 3 dp

        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");

        Assertions.assertThrows(
                ArithmeticException.class,
                () -> applicationCodeMapper.toApplicationCodeGetSummaryDto(code, fee, null));
    }

    @Test
    void emptyStringsAreMappedToNullInOutboundDtos() {
        Fee fee = new Fee();
        fee.setAmount(BigDecimal.valueOf(10.00));
        fee.setDescription("");
        fee.setReference("");

        Fee offsiteFee = new Fee();
        offsiteFee.setAmount(BigDecimal.valueOf(12.00));
        offsiteFee.setDescription("");
        offsiteFee.setReference("");

        ApplicationCode code = new ApplicationCode();
        code.setCode("");
        code.setTitle("");
        code.setWording("namely {TEXT|Specify Document Lost|100}");
        code.setBulkRespondentAllowed(YesOrNo.NO);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());

        var summaryDto =
                applicationCodeMapper.toApplicationCodeGetSummaryDto(code, fee, offsiteFee);
        var detailDto = applicationCodeMapper.toApplicationCodeGetDetailDto(code, fee, offsiteFee);

        Assertions.assertNull(summaryDto.getApplicationCode());
        Assertions.assertNull(summaryDto.getTitle());
        Assertions.assertTrue(summaryDto.getFeeReference().isPresent());
        Assertions.assertNull(summaryDto.getFeeReference().orElse("value"));
        Assertions.assertTrue(summaryDto.getFeeDescription().isPresent());
        Assertions.assertNull(summaryDto.getFeeDescription().orElse("value"));
        Assertions.assertTrue(summaryDto.getOffsiteFeeReference().isPresent());
        Assertions.assertNull(summaryDto.getOffsiteFeeReference().orElse("value"));
        Assertions.assertTrue(summaryDto.getOffsiteFeeDescription().isPresent());
        Assertions.assertNull(summaryDto.getOffsiteFeeDescription().orElse("value"));

        Assertions.assertNull(detailDto.getApplicationCode());
        Assertions.assertNull(detailDto.getTitle());
        Assertions.assertTrue(detailDto.getFeeReference().isPresent());
        Assertions.assertNull(detailDto.getFeeReference().orElse("value"));
        Assertions.assertTrue(detailDto.getFeeDescription().isPresent());
        Assertions.assertNull(detailDto.getFeeDescription().orElse("value"));
        Assertions.assertTrue(detailDto.getOffsiteFeeReference().isPresent());
        Assertions.assertNull(detailDto.getOffsiteFeeReference().orElse("value"));
        Assertions.assertTrue(detailDto.getOffsiteFeeDescription().isPresent());
        Assertions.assertNull(detailDto.getOffsiteFeeDescription().orElse("value"));
    }

    @Test
    void minAndMaxWithinNumeric92() {
        Fee min = new Fee();
        min.setAmount(new BigDecimal("0.00"));
        Fee max = new Fee();
        max.setAmount(new BigDecimal("9999999.99")); // 9,2 upper bound

        ApplicationCode code = new ApplicationCode();
        code.setCode("x");
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        applicationCodeMapper.setWordingTemplateMapper(new WordingTemplateMapperImpl());

        var dtoMin = applicationCodeMapper.toApplicationCodeGetSummaryDto(code, min, null);
        var dtoMax = applicationCodeMapper.toApplicationCodeGetSummaryDto(code, max, null);

        Assertions.assertEquals(0L, dtoMin.getFeeAmount().get().getValue());
        Assertions.assertEquals(999_999_999L, dtoMax.getFeeAmount().get().getValue()); // pence
    }
}
