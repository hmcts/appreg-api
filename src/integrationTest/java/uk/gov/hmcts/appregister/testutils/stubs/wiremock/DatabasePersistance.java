package uk.gov.hmcts.appregister.testutils.stubs.wiremock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntrySequenceMappingRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationRegisterRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;

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

    @Autowired private NationalCourtHouseRepository nationalCourtHouseRepository;

    @Autowired private NameAddressRepository nameAddressRepository;

    @Autowired private ApplicationRegisterRepository applicationRegisterRepository;

    @Autowired private StandardApplicantRepository standardApplicantRepository;

    @Autowired
    private ApplicationListEntryOfficialRepository applicationListEntryOfficialRepository;

    @Autowired private AppListEntryResolutionRepository appListEntryResolutionRepository;

    @Autowired private ResolutionCodeRepository resolutionCodeRepository;

    @Autowired private AppListEntrySequenceMappingRepository appListEntrySequenceMappingRepository;

    public ApplicationCode save(ApplicationCode data) {

        if (data.getApplicationListEntryList() != null) {
            data.getApplicationListEntryList()
                    .forEach(
                            e -> {
                                if (data.getId() == null) {
                                    applicationListEntryRepository.saveAndFlush(e);
                                }
                            });
        }

        return applicationCodeRepository.saveAndFlush(data);
    }

    public Fee save(Fee data) {
        return feeRepository.saveAndFlush(data);
    }

    public NameAddress save(NameAddress data) {
        return nameAddressRepository.saveAndFlush(data);
    }

    public ApplicationRegister save(ApplicationRegister data) {
        if (data.getApplicationList() != null) {
            save(data.getApplicationList());
        }

        ApplicationRegister register = applicationRegisterRepository.saveAndFlush(data);
        applicationRegisterRepository.flush();
        return register;
    }

    public DataAudit save(DataAudit data) {
        return dataAuditRepository.saveAndFlush(data);
    }

    public CriminalJusticeArea save(CriminalJusticeArea data) {
        return criminalJusticeAreaRepository.saveAndFlush(data);
    }

    public NationalCourtHouse save(NationalCourtHouse data) {
        return nationalCourtHouseRepository.saveAndFlush(data);
    }

    public ApplicationListEntry save(ApplicationListEntry entry) {

        if (entry.getApplicationCode() != null) {
            save(entry.getApplicationCode());
        }

        if (entry.getApplicationList() != null && entry.getApplicationList().getUuid() == null) {
            save(entry.getApplicationList());
        }

        if (entry.getStandardApplicant() != null) {
            save(entry.getStandardApplicant());
        }

        if (entry.getRnameaddress() != null) {
            save(entry.getRnameaddress());
        }

        if (entry.getAnamedaddress() != null) {
            save(entry.getAnamedaddress());
        }

        return applicationListEntryRepository.saveAndFlush(entry);
    }

    public ApplicationList save(ApplicationList entry) {
        if (entry.getCja() != null) {
            save(entry.getCja());
        }

        return applicationListRepository.saveAndFlush(entry);
    }

    public StandardApplicant save(StandardApplicant data) {
        return standardApplicantRepository.saveAndFlush(data);
    }

    public AppListEntryOfficial save(AppListEntryOfficial data) {
        return applicationListEntryOfficialRepository.saveAndFlush(data);
    }

    public AppListEntryResolution save(AppListEntryResolution entryResult) {

        if (entryResult.getApplicationList() != null) {
            save(entryResult.getApplicationList());
        }

        if (entryResult.getResolutionCode() != null) {
            save(entryResult.getResolutionCode());
        }

        return appListEntryResolutionRepository.saveAndFlush(entryResult);
    }

    public ResolutionCode save(ResolutionCode resolutionCode) {
        return resolutionCodeRepository.saveAndFlush(resolutionCode);
    }

    public AppListEntrySequenceMapping save(AppListEntrySequenceMapping data) {
        return appListEntrySequenceMappingRepository.saveAndFlush(data);
    }
}
