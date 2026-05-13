package uk.gov.hmcts.appregister.controller.reporting;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;

public class ReportingControllerSecurityTest extends AbstractSecurityControllerTest {

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        ReportingControllerGetTest.WEB_CONTEXT.formatted(
                                                UUID.randomUUID()),
                                        OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl("reports/list-maintenance/jobs"))
                        .method(HttpMethod.POST)
                        .payload(
                                new ListMaintenanceFilterDto()
                                        .dateFrom(LocalDate.of(2024, 1, 1))
                                        .dateTo(LocalDate.of(2026, 12, 31)))
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
