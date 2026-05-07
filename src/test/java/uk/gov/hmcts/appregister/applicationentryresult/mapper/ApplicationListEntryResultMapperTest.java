package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapperImpl;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;

public class ApplicationListEntryResultMapperTest {

    @Test
    public void testToApplicationListEntryResult() {
        ApplicationListEntryResultMapper applicationListEntryResultMapper =
                new ApplicationListEntryResultMapperImpl();
        applicationListEntryResultMapper.wordingTemplateMapper = new WordingTemplateMapperImpl();

        AppListEntryResolution appListEntryResolution = new AppListEntryResolution();
        ResolutionCode resolutionCode = new ResolutionCode();
        resolutionCode.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template "
                        + "{TEXT|Applicant officer2|20} and third\" +\n"
                        + "                            \"template {TEXT|Applicant officer3|30}");
        appListEntryResolution.setResolutionCode(resolutionCode);
        appListEntryResolution.setResolutionWording(
                "Test template {office1Val} and second template "
                        + "{office2Val} and third\" +\n"
                        + "                            \"template {office3Val}");
        ResultGetDto resultGetDt =
                applicationListEntryResultMapper.toResultGetDto(appListEntryResolution);

        Assertions.assertEquals(3, resultGetDt.getWording().getSubstitutionKeyConstraints().size());
        Assertions.assertEquals(
                "Applicant officer1",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "Applicant officer2",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(1).getKey());
        Assertions.assertEquals(
                "Applicant officer3",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(2).getKey());
        Assertions.assertEquals(
                "office1Val",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(0).getValue());
        Assertions.assertEquals(
                "office2Val",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(1).getValue());
        Assertions.assertEquals(
                "office3Val",
                resultGetDt.getWording().getSubstitutionKeyConstraints().get(2).getValue());
        Assertions.assertEquals(
                10,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getLength());
        Assertions.assertEquals(
                20,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(1)
                        .getConstraint()
                        .getLength());
        Assertions.assertEquals(
                30,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(2)
                        .getConstraint()
                        .getLength());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getType());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(1)
                        .getConstraint()
                        .getType());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                resultGetDt
                        .getWording()
                        .getSubstitutionKeyConstraints()
                        .get(2)
                        .getConstraint()
                        .getType());
    }
}
