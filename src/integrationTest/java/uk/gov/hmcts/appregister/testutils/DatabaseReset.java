package uk.gov.hmcts.appregister.testutils;

import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLCATION_LISTS_ENTRY_FEE_ID;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLCATION_LISTS_ENTRY_OFFICIAL;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLICATION_CODES;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLICATION_LISTS;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLICATION_LISTS_ENTRY;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLICATION_LISTS_FEE_STATUS;
import static uk.gov.hmcts.appregister.common.entity.TableNames.APPLICATION_LIST_ENTRY_SEQUENCE_MAPPING;
import static uk.gov.hmcts.appregister.common.entity.TableNames.ASYNC_JOBS;
import static uk.gov.hmcts.appregister.common.entity.TableNames.ASYNC_JOBS_APP_LIST_ENTRY;
import static uk.gov.hmcts.appregister.common.entity.TableNames.FEE;
import static uk.gov.hmcts.appregister.common.entity.TableNames.RESOLUTION_CODES;
import static uk.gov.hmcts.appregister.common.entity.TableNames.STANDARD_APPLICANTS;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A global persistence class that knows how to persist objects. Specifically ones that have been
 * created using the {@link uk.gov.hmcts.appregister.testutils.data.Persistable}
 */
@Component
public class DatabaseReset {
    @PersistenceContext private EntityManager entityManager;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchema;

    /**
     * A bit crude but a sequence number that manages the data beyond all the baseline data sequence
     * numbers. This means we can manage data targeted around specific tests.
     */
    public static final int SEQUENCE_START_VALUE = 321364044;

    @Transactional
    public void resetDbData() {
        resetSequences();

        deleteAll(APPLCATION_LISTS_ENTRY_FEE_ID);
        deleteByIdGreaterThanOrEqual(APPLICATION_LISTS_FEE_STATUS, "alefs_id");
        deleteByIdGreaterThanOrEqual(APPLCATION_LISTS_ENTRY_OFFICIAL, "aleo_id");
        deleteByIdGreaterThanOrEqual(ASYNC_JOBS_APP_LIST_ENTRY, "aj_ale_id");
        deleteByIdGreaterThanOrEqual("application_register", "ar_id");
        deleteByIdGreaterThanOrEqual("app_list_entry_resolutions", "aler_id");
        deleteByIdGreaterThanOrEqual(FEE, "fee_id");
        deleteByIdGreaterThanOrEqual(RESOLUTION_CODES, "rc_id");
        deleteByIdGreaterThanOrEqual(APPLICATION_LISTS_ENTRY, "ale_id");
        deleteByIdGreaterThanOrEqual(APPLICATION_LIST_ENTRY_SEQUENCE_MAPPING, "al_id");
        deleteByIdGreaterThanOrEqual("name_address", "na_id");
        deleteByIdGreaterThanOrEqual(APPLICATION_CODES, "ac_id");
        deleteByIdGreaterThanOrEqual(APPLICATION_LISTS, "al_id");
        deleteByIdGreaterThanOrEqual("criminal_justice_area", "cja_id");
        deleteByIdGreaterThanOrEqual("national_court_houses", "nch_id");
        deleteAll("national_court_houses_staging");
        deleteByIdGreaterThanOrEqual(STANDARD_APPLICANTS, "sa_id");
        deleteAll("standard_applicants_staging");
        deleteByIdGreaterThanOrEqual(ASYNC_JOBS, "aj_id");
        deleteAll("data_audit");
    }

    @Transactional
    public void resetSequences() {
        final Query query =
                entityManager.createNativeQuery(
                        "SELECT sequence_name FROM information_schema.sequences "
                                + "WHERE sequence_schema = '"
                                + sqlInitSchema
                                + "'");
        final List<?> sequences = query.getResultList();
        for (Object seqName : sequences) {
            entityManager
                    .createNativeQuery(
                            "ALTER SEQUENCE "
                                    + sqlInitSchema
                                    + "."
                                    + seqName
                                    + " RESTART WITH "
                                    + SEQUENCE_START_VALUE)
                    .executeUpdate();
        }
    }

    private void deleteByIdGreaterThanOrEqual(String tableName, String idColumn) {
        entityManager
                .createNativeQuery(
                        "DELETE FROM "
                                + qualifiedTableName(tableName)
                                + " WHERE "
                                + idColumn
                                + " >= "
                                + SEQUENCE_START_VALUE)
                .executeUpdate();
    }

    private void deleteAll(String tableName) {
        entityManager
                .createNativeQuery("DELETE FROM " + qualifiedTableName(tableName))
                .executeUpdate();
    }

    private String qualifiedTableName(String tableName) {
        return sqlInitSchema + "." + tableName;
    }
}
