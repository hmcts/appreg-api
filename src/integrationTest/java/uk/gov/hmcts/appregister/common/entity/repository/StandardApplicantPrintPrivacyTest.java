package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

class StandardApplicantPrintPrivacyTest extends BaseRepositoryTest {
    @Autowired private ApplicationListEntryRepository entries;
    @Autowired private TransactionalUnitOfWork work;

    @Test
    void bothPrintQueriesExcludeStandardApplicantPersonalDetailsButPreserveOrdinaryApplicants()
            throws Exception {
        work.inTransaction(
                () -> {
                    var entry = entries.findAll().getFirst();
                    var originalStandard = entry.getStandardApplicant();
                    var originalApplicant = entry.getAnamedaddress();
                    try {
                        var standard =
                                persistance.save(new StandardApplicantTestData().someComplete());
                        entry.setStandardApplicant(standard);
                        entry.setAnamedaddress(null);
                        entries.saveAndFlush(entry);
                        var listId = entry.getApplicationList().getUuid();
                        var id = entry.getId();
                        for (var rows :
                                List.of(
                                        entries.findByIdForPrinting(listId),
                                        entries.findByApplicationListIdsForPrinting(
                                                List.of(listId),
                                                false,
                                                List.of(entry.getUuid())))) {
                            var row =
                                    rows.stream()
                                            .filter(item -> item.getId().equals(id))
                                            .findFirst()
                                            .orElseThrow();
                            assertThat(row.getApplicantName()).isEqualTo(standard.getName());
                            assertPrivateFieldsAbsent(row);
                        }
                        var ordinary = persistance.save(new NameAddressTestData().someComplete());
                        entry.setStandardApplicant(null);
                        entry.setAnamedaddress(ordinary);
                        entries.saveAndFlush(entry);
                        for (var rows :
                                List.of(
                                        entries.findByIdForPrinting(listId),
                                        entries.findByApplicationListIdsForPrinting(
                                                List.of(listId),
                                                false,
                                                List.of(entry.getUuid())))) {
                            var row =
                                    rows.stream()
                                            .filter(item -> item.getId().equals(id))
                                            .findFirst()
                                            .orElseThrow();
                            assertThat(row.getApplicantAddressLine1())
                                    .isEqualTo(ordinary.getAddress1());
                            assertThat(row.getApplicantEmail())
                                    .isEqualTo(ordinary.getEmailAddress());
                            assertThat(row.getApplicantFirstName())
                                    .isEqualTo(ordinary.getFirstName());
                        }
                    } finally {
                        entry.setStandardApplicant(originalStandard);
                        entry.setAnamedaddress(originalApplicant);
                        entries.saveAndFlush(entry);
                    }
                });
    }

    private static void assertPrivateFieldsAbsent(ApplicationListEntryPrintProjection row) {
        assertThat(row)
                .extracting(
                        ApplicationListEntryPrintProjection::getApplicantTitle,
                        ApplicationListEntryPrintProjection::getApplicantFirstName,
                        ApplicationListEntryPrintProjection::getApplicantMiddleName,
                        ApplicationListEntryPrintProjection::getApplicantLastName,
                        ApplicationListEntryPrintProjection::getApplicantAddressLine1,
                        ApplicationListEntryPrintProjection::getApplicantAddressLine2,
                        ApplicationListEntryPrintProjection::getApplicantAddressLine3,
                        ApplicationListEntryPrintProjection::getApplicantAddressLine4,
                        ApplicationListEntryPrintProjection::getApplicantAddressLine5,
                        ApplicationListEntryPrintProjection::getApplicantPostcode,
                        ApplicationListEntryPrintProjection::getApplicantPhone,
                        ApplicationListEntryPrintProjection::getApplicantMobile,
                        ApplicationListEntryPrintProjection::getApplicantEmail)
                .containsOnlyNulls();
    }
}
