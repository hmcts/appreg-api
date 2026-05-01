package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

class FeesReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<FeesReportRow>> pages = new ArrayList<>();
        PageReader<FeesReportRow> pageReader =
                (rows, context) -> {
                    Assertions.assertSame(jobContext, context);
                    pages.add(rows);
                };
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();
        List<String> queries = new ArrayList<>();
        AtomicInteger queryCount = new AtomicInteger();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<FeesReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<FeesReportRow> rowMapper = invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        FeesReportFilterDto filter = filter();
        FeesReportDataReader reader = new FeesReportDataReader(jdbcTemplate, filter, "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        FeesReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(LocalDate.of(2018, 5, 18), row.getListDate());
        Assertions.assertEquals("B01IX00 - Westminster", row.getCourthouseName());
        Assertions.assertEquals("Other court", row.getOtherCourthouse());
        Assertions.assertEquals("01", row.getCjaCode());
        Assertions.assertEquals("STD1", row.getStandardApplicantCode());
        Assertions.assertEquals("British Gas", row.getApplicantFullName());
        Assertions.assertEquals("RE99001", row.getApplicationCode());
        Assertions.assertEquals("Rights of Entry Warrant", row.getApplicationCodeTitle());
        Assertions.assertEquals(BigDecimal.valueOf(20), row.getFeeValue());
        Assertions.assertEquals(BigDecimal.valueOf(1), row.getOffSiteFeeValue());
        Assertions.assertEquals(BigDecimal.valueOf(21), row.getTotalFeeValue());
        Assertions.assertEquals("Due", row.getFeeStatus());
        Assertions.assertEquals(LocalDate.of(2018, 12, 3), row.getFeeStatusDate());
        Assertions.assertEquals("REF-1", row.getPaymentReference());

        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertKeysetPredicateRunsBeforeFeeCtes(queries.getFirst());
        assertLegacyFeesQueryShape(queries.getFirst());
    }

    @Test
    void givenNoLocation_whenReadData_thenLocationParametersAreNull() throws IOException {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();
        List<String> queries = new ArrayList<>();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<FeesReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            return List.of();
                        });

        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));
        FeesReportDataReader reader = new FeesReportDataReader(jdbcTemplate, filter, "appreg");
        PageReader<FeesReportRow> pageReader =
                (rows, context) -> Assertions.fail("No rows expected");

        reader.readData(new ReadPagePosition(25, 5), pageReader, mock(JobContext.class));

        MapSqlParameterSource parameters = parameterSources.getFirst();
        Assertions.assertNull(parameters.getValue("cjaCode"));
        Assertions.assertNull(parameters.getValue("otherCourthouse"));
        Assertions.assertNull(parameters.getValue("courthouseCode"));
        assertKeysetPredicateRunsBeforeFeeCtes(queries.getFirst());
    }

    @Test
    void givenShortPage_whenReadData_thenStopsWithoutEmptyRead() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<FeesReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<FeesReportRow> rowMapper = invocation.getArgument(2);
                            return List.of(rowMapper.mapRow(resultSet(), 0));
                        });

        FeesReportDataReader reader = new FeesReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(
                new ReadPagePosition(25, 0),
                (rows, context) -> Assertions.assertEquals(1, rows.size()),
                mock(JobContext.class));

        Assertions.assertEquals(1, parameterSources.size());
        Assertions.assertEquals(25, parameterSources.getFirst().getValue("limit"));
    }

    private void assertParameters(MapSqlParameterSource parameters, boolean expectedCursor) {
        Assertions.assertEquals(LocalDate.of(2018, 5, 1), parameters.getValue("dateFrom"));
        Assertions.assertEquals(LocalDate.of(2018, 5, 31), parameters.getValue("dateTo"));
        Assertions.assertEquals("STD1", parameters.getValue("standardApplicantCode"));
        Assertions.assertEquals("John Smith", parameters.getValue("applicantName"));
        Assertions.assertEquals("British Gas", parameters.getValue("applicantOrganisation"));
        Assertions.assertEquals("01", parameters.getValue("cjaCode"));
        Assertions.assertEquals("Other court", parameters.getValue("otherCourthouse"));
        Assertions.assertEquals("B01IX00", parameters.getValue("courthouseCode"));
        Assertions.assertEquals(1, parameters.getValue("limit"));
        Assertions.assertEquals(expectedCursor, parameters.getValue("hasCursor"));

        if (expectedCursor) {
            Assertions.assertEquals(LocalDate.of(2018, 5, 18), parameters.getValue("lastListDate"));
            Assertions.assertEquals(123L, parameters.getValue("lastApplicationListEntryId"));
        } else {
            Assertions.assertNull(parameters.getValue("lastListDate"));
            Assertions.assertNull(parameters.getValue("lastApplicationListEntryId"));
        }
    }

    private FeesReportFilterDto filter() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .cjaCode("01")
                        .otherLocationDescription("Other court")
                        .courtLocationCode("B01IX00");
        return new FeesReportFilterDto()
                .dateFrom(LocalDate.of(2018, 5, 1))
                .dateTo(LocalDate.of(2018, 5, 31))
                .standardApplicantCode("STD1")
                .applicantName("John Smith")
                .applicantOrganisation("British Gas")
                .location(location);
    }

    private void assertKeysetPredicateRunsBeforeFeeCtes(String query) {
        int cursorPredicateIndex = query.indexOf(":hasCursor IS FALSE");
        int latestFeeStatusIndex = query.indexOf("latest_fee_status AS");
        int finalSelectIndex = query.indexOf("fa.application_list_date AS list_date");

        Assertions.assertTrue(cursorPredicateIndex > -1);
        Assertions.assertTrue(latestFeeStatusIndex > -1);
        Assertions.assertTrue(finalSelectIndex > -1);
        Assertions.assertTrue(cursorPredicateIndex < latestFeeStatusIndex);
        Assertions.assertEquals(-1, query.indexOf(":hasCursor IS FALSE", finalSelectIndex));
        Assertions.assertTrue(query.contains("EXISTS ("));
        Assertions.assertTrue(query.contains("FROM app_list_entry_fee_id cursor_alefi"));
    }

    private void assertLegacyFeesQueryShape(String query) {
        String normalisedQuery = query.replaceAll("\\s+", " ");
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(sa.standard_applicant_code) "
                                + "LIKE '%' || UPPER(:standardApplicantCode) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(b.other_courthouse) "
                                + "LIKE '%' || UPPER(:otherCourthouse) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(b.courthouse_code) " + "LIKE '%' || UPPER(:courthouseCode) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains("UPPER(SUBSTRING(b.courthouse_code FROM 2 FOR 2))"));
        Assertions.assertTrue(normalisedQuery.contains("AND :otherCourthouse IS NULL"));
        Assertions.assertTrue(normalisedQuery.contains("AND :courthouseCode IS NULL"));
        Assertions.assertEquals(-1, normalisedQuery.indexOf("UPPER(b.courthouse_code) = UPPER"));
        Assertions.assertEquals(-1, normalisedQuery.indexOf("UPPER(sa.standard_applicant_code) ="));
    }

    private ResultSet resultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("ale_id")).thenReturn(123L);
        when(resultSet.getObject("list_date", LocalDate.class))
                .thenReturn(LocalDate.of(2018, 5, 18));
        when(resultSet.getString("courthouse_name")).thenReturn("B01IX00 - Westminster");
        when(resultSet.getString("other_courthouse")).thenReturn("Other court");
        when(resultSet.getString("cja_code")).thenReturn("01");
        when(resultSet.getString("standard_applicant_code")).thenReturn("STD1");
        when(resultSet.getString("applicant_full_name")).thenReturn("British Gas");
        when(resultSet.getString("application_code")).thenReturn("RE99001");
        when(resultSet.getString("application_code_title")).thenReturn("Rights of Entry Warrant");
        when(resultSet.getBigDecimal("fee_value")).thenReturn(BigDecimal.valueOf(20));
        when(resultSet.getBigDecimal("off_site_fee_value")).thenReturn(BigDecimal.ONE);
        when(resultSet.getBigDecimal("total_fee_value")).thenReturn(BigDecimal.valueOf(21));
        when(resultSet.getString("fee_status")).thenReturn("Due");
        when(resultSet.getObject("fee_status_date", LocalDate.class))
                .thenReturn(LocalDate.of(2018, 12, 3));
        when(resultSet.getString("payment_reference")).thenReturn("REF-1");
        return resultSet;
    }
}
