package uk.gov.hmcts.appregister.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "uk.gov.hmcts.appregister", importOptions = { ImportOption.DoNotIncludeTests.class })
public class BaseRules {
    protected static final String BASE_PACKAGE = "uk.gov.hmcts.appregister";
}
