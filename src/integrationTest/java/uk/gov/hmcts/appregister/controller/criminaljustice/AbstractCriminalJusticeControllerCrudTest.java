package uk.gov.hmcts.appregister.controller.criminaljustice;

import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

public class AbstractCriminalJusticeControllerCrudTest extends BaseIntegration {
    protected static final String WEB_CONTEXT = "criminal-justice-areas";

    // expectations based on the flyway test data
    protected static final String EXPECTED_CODE = "CD";
    protected static final String EXPECTED_DESCRIPTION = "CJA_CD_DESCRIPTION";

    protected static final String EXPECTED_CODE1 = "CE";
    protected static final String EXPECTED_DESCRIPTION1 = "CJA_CE_DESCRIPTION";

    protected static final String EXPECTED_CODE2 = "CJ";
    protected static final String EXPECTED_DESCRIPTION2 = "CJA_DESCRIPTION";

    // audit expectations
    protected static final String EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION =
            CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getEventName();
    protected static final String EXPECTED_GET_CRIMINAL_JUSTICE_AREAS_AUDIT_ACTION =
            CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT.getEventName();

    protected static final Integer DEFAULT_PAGE_SIZE = 10;

    // The total criminal justice area inserted by flyway scripts. See V6__InitialTestData.sql
    protected static final int TOTAL_CJA_COUNT = 4;
}
