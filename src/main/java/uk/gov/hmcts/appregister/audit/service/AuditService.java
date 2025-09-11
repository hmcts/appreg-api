package uk.gov.hmcts.appregister.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.AuditEnum;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;

@Component
@RequiredArgsConstructor
public class AuditService {
    private final DataAuditRepository dataAuditRepository;

    private final UserProvider authenticatedUser;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    private final HttpServletRequest request;

    @Transactional
    public void record(AuditEnum auditEnum) {
        DataAudit dataAudit = new DataAudit();

        dataAudit.setColumnName(auditEnum.getColumnName());
        dataAudit.setTableName(auditEnum.getTableName());
        dataAudit.setUserName(authenticatedUser.getUser());
        dataAudit.setEventName(auditEnum.getEventName());

        // TODO: What should these values be
        dataAudit.setUpdateType("1");
        dataAudit.setSchemaName(sqlInitSchemaLocations);

        dataAudit.setLink(request.getRequestURI());
        dataAuditRepository.save(dataAudit);
    }
}
