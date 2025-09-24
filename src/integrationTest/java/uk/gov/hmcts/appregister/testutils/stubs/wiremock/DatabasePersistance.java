package uk.gov.hmcts.appregister.testutils.stubs.wiremock;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
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

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private CriminalJusticeAreaRepository criminalJusticeAreaRepository;

    @Transactional
    public ApplicationCode save(ApplicationCode data) {

        if (data.getApplicationListEntryList() != null) {
            data.getApplicationListEntryList()
                    .forEach(
                            e -> {
                                if (data.getId() == null) {
                                    applicationListEntryRepository.save(e);
                                }
                            });
        }

        return applicationCodeRepository.save(data);
    }

    @Transactional
    public Fee save(Fee data) {
        return feeRepository.save(data);
    }

    @Transactional
    public DataAudit save(DataAudit data) {
        return dataAuditRepository.save(data);
    }

    @Transactional
    public CriminalJusticeArea save(CriminalJusticeArea data) {
        return criminalJusticeAreaRepository.save(data);
    }

    @Transactional
    public ApplicationListEntry save(ApplicationListEntry entry) {

        if (entry.getApplicationCode() != null) {
            save(entry.getApplicationCode());
        }

        if (entry.getApplicationList() != null && entry.getApplicationList().getId() == null) {
            applicationListRepository.save(entry.getApplicationList());
        }

        return applicationListEntryRepository.save(entry);
    }

    @Transactional
    public ApplicationList save(ApplicationList entry) {
        ApplicationList savedEntry = applicationListRepository.save(entry);

        // save all entries
        if (savedEntry.getEntries() != null) {
            savedEntry
                    .getEntries()
                    .forEach(
                            e -> {
                                if (savedEntry.getId() == null) {
                                    applicationListEntryRepository.save(e);
                                }
                            });
        }

        return entry;
    }
}
