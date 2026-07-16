package uk.gov.hmcts.appregister.csds.ingress.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@Component
@RequiredArgsConstructor
public class CsdsAuditService {
    private static final String AUDIT_CSDS_PARAMETER = "AUDIT_CSDS";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CsdsAuditWriteService csdsAuditWriteService;
    private final CsdsAuditFailurePersistenceService csdsAuditFailurePersistenceService;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public CsdsAuditLevel auditLevel() {
        try {
            var sql =
                    """
                    SELECT parameter_value
                    FROM %s.configuration_parameters
                    WHERE parameter_name = :parameterName
                    """
                            .formatted(schema);
            var value =
                    jdbcTemplate.queryForObject(
                            sql,
                            new MapSqlParameterSource("parameterName", AUDIT_CSDS_PARAMETER),
                            String.class);
            if (!StringUtils.hasText(value)) {
                throw new IllegalArgumentException("AUDIT_CSDS parameter value must not be blank");
            }
            return CsdsAuditLevel.fromDatabaseValue(value);
        } catch (EmptyResultDataAccessException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "AUDIT_CSDS configuration parameter is missing",
                    ex);
        } catch (IllegalArgumentException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "AUDIT_CSDS configuration parameter is invalid",
                    ex);
        }
    }

    public void persistSuccessAudits(CsdsAuditLevel level, java.util.List<CsdsAuditEntry> audits) {
        if (level.auditsSuccesses()) {
            csdsAuditWriteService.persist(audits);
        }
    }

    public void persistFailureAudits(CsdsAuditLevel level, java.util.List<CsdsAuditEntry> audits) {
        if (level.auditsFailures()) {
            csdsAuditFailurePersistenceService.persist(audits);
        }
    }
}
