package uk.gov.hmcts.appregister.controller.resultcode;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.resultcode.audit.ResultCodeAuditOperation;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

public class AbstractResultCodeControllerCrudTest extends BaseIntegration {
    protected static final String WEB_CONTEXT = "result-codes";

    // Known seeds (from your resolution_codes seed data)
    protected static final String APPC_CODE = "APPC";
    protected static final String APPC_TITLE = "Appeal to Crown Court";
    protected static final String AUTH_CODE = "AUTH";
    protected static final String AUTH_TITLE = "Authorised";
    protected static final String CASE_CODE = "CASE";

    protected static final LocalDate SEED_START = LocalDate.of(2016, 1, 1);
    protected static final LocalDate ACTIVE_DAY = LocalDate.of(2025, 1, 1);

    // Audit event names
    protected static final String AUDIT_GET_ONE =
            ResultCodeAuditOperation.GET_RESULT_CODE_AUDIT_EVENT.getEventName();
    protected static final String AUDIT_GET_PAGE =
            ResultCodeAuditOperation.GET_RESULT_CODES_AUDIT_EVENT.getEventName();

    protected static final int DEFAULT_PAGE_SIZE = 10;

    // --- /result-codes/{code}?date=YYYY-MM-DD -----------------------------------------------
}
