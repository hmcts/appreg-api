package uk.gov.hmcts.appregister.testutils.stub;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;

/**
 * A persistence class that knows how to install the data into the database in the correct order.
 */
@Component
public class DatabasePersistance {
    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private FeeRepository feeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Transactional
    public ApplicationCode saveAppCode(ApplicationCode data) {
        return applicationCodeRepository.save(data);
    }

    @Transactional
    public Fee saveFee(Fee data) {
        return feeRepository.save(data);
    }

    @Transactional
    public DataAudit saveAudit(DataAudit data) {
        return dataAuditRepository.save(data);
    }
}
