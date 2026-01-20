package uk.gov.hmcts.appregister.applicationcode.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;

public class ApplicationCodeMapperTest {

    private final ApplicationCodeMapper applicationCodeMapper = new ApplicationCodeMapperImpl();

    @Test
    public void testWithCompleteMapApplicationCodeGetSummaryDto() {
        Fee fee = new Fee();
        fee.setAmount(BigDecimal.valueOf(232.34));
        fee.setDescription("Description");
        fee.setOffsite(false);

        Fee offsitefee = new Fee();
        offsitefee.setAmount(BigDecimal.valueOf(23666.34));
        offsitefee.setDescription("Description offset");
        offsitefee.setOffsite(true);

        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(LocalDate.now());
        code.setStartDate(LocalDate.now());
        code.setFeeReference("reference");
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
        code.setWording("namely {TEXT|Specify Document Lost|100}");

        ApplicationCodeGetSummaryDto summaryDto =
                applicationCodeMapper.toApplicationCodeGetSummaryDto(code, fee, offsitefee);

        // assert
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
                ApplicationCodeGetSummaryDtoFeeAmount.CurrencyEnum.GBP,
                summaryDto.getOffsiteFeeAmount().get().getCurrency());
        Assertions.assertEquals("reference", summaryDto.getFeeReference().get());
        Assertions.assertEquals(Boolean.FALSE, summaryDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, summaryDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, summaryDto.getBulkRespondentAllowed());
        Assertions.assertEquals("Description", summaryDto.getFeeDescription().get());
    }

    @Test
    public void testWithoutFeesMapApplicationCodeGetSummaryDto() {
        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(LocalDate.now());
        code.setStartDate(LocalDate.now());
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
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
    public void testWithCompleteMapApplicationCodeGetDetailDto() {
        Fee fee = new Fee();
        fee.setAmount(BigDecimal.valueOf(232.34));
        fee.setDescription("Description");
        fee.setOffsite(false);

        Fee offsetfee = new Fee();
        offsetfee.setAmount(BigDecimal.valueOf(23666.34));
        offsetfee.setDescription("Description offset");
        offsetfee.setOffsite(true);

        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(LocalDate.now());
        code.setStartDate(LocalDate.now());
        code.setFeeReference("reference");
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
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
                ApplicationCodeGetSummaryDtoFeeAmount.CurrencyEnum.GBP,
                getDetailDto.getOffsiteFeeAmount().get().getCurrency());
        Assertions.assertEquals("reference", getDetailDto.getFeeReference().get());
        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getIsFeeDue());
        Assertions.assertEquals(Boolean.FALSE, getDetailDto.getRequiresRespondent());
        Assertions.assertEquals(Boolean.TRUE, getDetailDto.getBulkRespondentAllowed());
        Assertions.assertEquals("Description", getDetailDto.getFeeDescription().get());
    }

    @Test
    public void testWithoutFeesMapApplicationCodeGetDetailDto() {
        ApplicationCode code = new ApplicationCode();
        code.setCode("appcode");
        code.setEndDate(LocalDate.now());
        code.setStartDate(LocalDate.now());
        code.setBulkRespondentAllowed(YesOrNo.YES);
        code.setRequiresRespondent(YesOrNo.NO);
        code.setFeeDue(YesOrNo.NO);
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
    void minAndMaxWithinNumeric92() {
        Fee min = new Fee();
        min.setAmount(new BigDecimal("0.00"));
        Fee max = new Fee();
        max.setAmount(new BigDecimal("9999999.99")); // 9,2 upper bound

        ApplicationCode code = new ApplicationCode();
        code.setCode("x");

        var dtoMin = applicationCodeMapper.toApplicationCodeGetSummaryDto(code, min, null);
        var dtoMax = applicationCodeMapper.toApplicationCodeGetSummaryDto(code, max, null);

        Assertions.assertEquals(0L, dtoMin.getFeeAmount().get().getValue());
        Assertions.assertEquals(999_999_999L, dtoMax.getFeeAmount().get().getValue()); // pence
    }
}
