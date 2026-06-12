package uk.gov.hmcts.appregister.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.common.async.exception.JobException;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea_;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse_;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.base.BaseDeletableEntity;
import uk.gov.hmcts.appregister.common.entity.compositeid.AppListEntryFeeCompositeId;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;
import uk.gov.hmcts.appregister.common.template.type.DateType;

class CommonValueTypesCoverageTest {

    @Test
    void appListEntryFeeCompositeId_supportsBothConstructorsAndMutators() {
        var empty = new AppListEntryFeeCompositeId();
        empty.setAppListEntryId(1L);
        empty.setFeeId(2L);

        assertEquals(1L, empty.getAppListEntryId());
        assertEquals(2L, empty.getFeeId());

        var populated = new AppListEntryFeeCompositeId(3L, 4L);
        assertEquals(3L, populated.getAppListEntryId());
        assertEquals(4L, populated.getFeeId());
    }

    @Test
    void auditEventEnum_exposesConfiguredMetadata() {
        assertEquals(8, AuditEventEnum.values().length);
        assertEquals(
                TableNames.APPLICATION_CODES,
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT.getTableName());
        assertEquals(
                ApplicationCode_.CODE,
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT.getColumnName());
        assertEquals(
                "Get Application Code",
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT.getEventName());
        assertEquals(
                CriminalJusticeArea_.CODE,
                AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getColumnName());
        assertEquals(
                NationalCourtHouse_.NAME,
                AuditEventEnum.GET_COURT_LOCATION_AUDIT_EVENT.getColumnName());
    }

    @Test
    void baseDeletableEntity_storesDeletionState() {
        var deletedDate = OffsetDateTime.parse("2026-06-11T10:15:30Z");
        var entity = new BaseDeletableEntity();

        entity.setDeletedBy("tester");
        entity.setDeletedDate(deletedDate);
        entity.setDeleted(YesOrNo.YES);

        assertEquals("tester", entity.getDeletedBy());
        assertSame(deletedDate, entity.getDeletedDate());
        assertEquals(YesOrNo.YES, entity.getDeleted());
    }

    @Test
    void dateType_currentlyAcceptsAnyValue() {
        assertTrue(new DateType().validateForType("not-a-date"));
    }

    @Test
    void errorDetail_getTypeReturnsUriWhenAppCodePresent() {
        var errorDetail =
                new ErrorDetail() {
                    @Override
                    public HttpStatus getHttpCode() {
                        return HttpStatus.BAD_REQUEST;
                    }

                    @Override
                    public String getMessage() {
                        return "bad request";
                    }

                    @Override
                    public String getAppCode() {
                        return "urn:test:error";
                    }
                };

        assertEquals(URI.create("urn:test:error"), errorDetail.getType().orElseThrow());
    }

    @Test
    void errorDetail_getTypeReturnsEmptyWhenAppCodeMissing() {
        var errorDetail =
                new ErrorDetail() {
                    @Override
                    public HttpStatus getHttpCode() {
                        return HttpStatus.BAD_REQUEST;
                    }

                    @Override
                    public String getMessage() {
                        return "bad request";
                    }

                    @Override
                    public String getAppCode() {
                        return null;
                    }
                };

        assertTrue(errorDetail.getType().isEmpty());
    }

    @Test
    void jobException_supportsMessageAndCause() {
        var cause = new IllegalStateException("broken");
        var exceptionWithCause = new JobException("failed", cause);
        var exceptionWithoutCause = new JobException("failed");

        assertEquals("failed", exceptionWithCause.getMessage());
        assertSame(cause, exceptionWithCause.getCause());
        assertEquals("failed", exceptionWithoutCause.getMessage());
        assertNull(exceptionWithoutCause.getCause());
    }

    @Test
    void asyncJob_mapsChangedByAndChangedDateToUserFields() {
        var changedDate = OffsetDateTime.parse("2026-06-11T10:15:30Z");
        var asyncJob = AsyncJob.builder().id(5L).jobState(JobStatusType.SUBMITTED).build();

        asyncJob.setChangedBy("tester");
        asyncJob.setChangedDate(changedDate);

        assertEquals("tester", asyncJob.getChangedBy());
        assertSame(changedDate, asyncJob.getChangedDate());
        assertEquals("tester", asyncJob.getUserName());
        assertSame(changedDate, asyncJob.getUpdateTime());
    }
}
