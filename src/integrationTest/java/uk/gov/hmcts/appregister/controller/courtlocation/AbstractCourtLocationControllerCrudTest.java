package uk.gov.hmcts.appregister.controller.courtlocation;

import java.time.LocalDate;
import java.time.Month;
import uk.gov.hmcts.appregister.courtlocation.audit.CourtLocationAuditOperation;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

class AbstractCourtLocationControllerCrudTest extends BaseIntegration {
    protected static final String WEB_CONTEXT = "court-locations";

    protected static final String CARDIFF_CODE = "C62CR00";
    protected static final String CARDIFF_NAME = "Cardiff Crown Court";
    protected static final LocalDate CARDIFF_START = LocalDate.of(2016, Month.JANUARY, 1);

    protected static final String BRISTOL_CODE = "C52BT00";
    protected static final String BRISTOL_NAME = "Bristol Crown Court";
    protected static final LocalDate BRISTOL_START = LocalDate.of(2016, Month.JANUARY, 1);

    // Audit event names
    protected static final String AUDIT_GET_ONE =
            CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT.getEventName();
    protected static final String AUDIT_GET_PAGE =
            CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT.getEventName();

    protected static final int DEFAULT_PAGE_SIZE = 10;
}
