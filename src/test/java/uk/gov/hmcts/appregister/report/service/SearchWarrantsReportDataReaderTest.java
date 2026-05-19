package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.report.model.SearchWarrantsReportRow;

class SearchWarrantsReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<SearchWarrantsReportRow>> pages = new ArrayList<>();
        PageReader<SearchWarrantsReportRow> pageReader =
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
                        ArgumentMatchers.<RowMapper<SearchWarrantsReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<SearchWarrantsReportRow> rowMapper =
                                    invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        SearchWarrantsReportFilterDto filter = filter();
        SearchWarrantsReportDataReader reader =
                new SearchWarrantsReportDataReader(jdbcTemplate, filter, "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        SearchWarrantsReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(LocalDate.of(2018, 5, 18), row.getListDate());
        Assertions.assertEquals("B01IX00 - Westminster", row.getCourthouseName());
        Assertions.assertEquals("Other court", row.getOtherCourthouse());
        Assertions.assertEquals("01", row.getCjaCode());
        Assertions.assertEquals("STD1", row.getStandardApplicantCode());
        Assertions.assertEquals("British Gas", row.getApplicantFullName());
        Assertions.assertEquals("RE99001", row.getApplicationCode());
        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertKeysetPredicateRunsBeforeSearchWarrantsCtes(queries.getFirst());
        assertLegacySearchWarrantsQueryShape(queries.getFirst());
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
                        ArgumentMatchers.<RowMapper<SearchWarrantsReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            return List.of();
                        });

        SearchWarrantsReportFilterDto filter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));
        SearchWarrantsReportDataReader reader =
                new SearchWarrantsReportDataReader(jdbcTemplate, filter, "appreg");
        PageReader<SearchWarrantsReportRow> pageReader =
                (rows, context) -> Assertions.fail("No rows expected");

        reader.readData(new ReadPagePosition(25, 5), pageReader, mock(JobContext.class));

        MapSqlParameterSource parameters = parameterSources.getFirst();
        Assertions.assertNull(parameters.getValue("cjaCode"));
        Assertions.assertNull(parameters.getValue("otherCourthouse"));
        Assertions.assertNull(parameters.getValue("courthouseCode"));
        assertKeysetPredicateRunsBeforeSearchWarrantsCtes(queries.getFirst());
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
                        ArgumentMatchers.<RowMapper<SearchWarrantsReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<SearchWarrantsReportRow> rowMapper =
                                    invocation.getArgument(2);
                            return List.of(rowMapper.mapRow(resultSet(), 0));
                        });

        SearchWarrantsReportDataReader reader =
                new SearchWarrantsReportDataReader(jdbcTemplate, filter(), "appreg");
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

    private SearchWarrantsReportFilterDto filter() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .cjaCode("01")
                        .otherLocationDescription("Other court")
                        .courtLocationCode("B01IX00");
        return new SearchWarrantsReportFilterDto()
                .dateFrom(LocalDate.of(2018, 5, 1))
                .dateTo(LocalDate.of(2018, 5, 31))
                .location(location);
    }

    private void assertKeysetPredicateRunsBeforeSearchWarrantsCtes(String query) {
        int cursorPredicateIndex = query.indexOf(":hasCursor IS FALSE");
        int latestReplaceIndex =
                query.indexOf("REPLACE(REPLACE(ale.application_list_entry_wording");
        int finalSelectIndex = query.indexOf("c.ale_id");

        Assertions.assertTrue(cursorPredicateIndex > -1);
        Assertions.assertTrue(latestReplaceIndex > -1);
        Assertions.assertTrue(finalSelectIndex > -1);
        Assertions.assertTrue(query.contains("FROM application_lists al"));
    }

    private void assertLegacySearchWarrantsQueryShape(String query) {
        String normalisedQuery = query.replaceAll("\\s+", " ");

        Assertions.assertTrue(normalisedQuery.contains("WITH candidate_apps AS ("));
        Assertions.assertTrue(normalisedQuery.contains("UPPER(ac.application_code) LIKE 'SW%'"));
        Assertions.assertTrue(normalisedQuery.contains("application_list_entry_wording"));
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(c.other_courthouse) LIKE '%' || UPPER(:otherCourthouse) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(c.courthouse_code) LIKE '%' || UPPER(:courthouseCode) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains("UPPER(SUBSTRING(c.courthouse_code FROM 2 FOR 2))"));
        Assertions.assertTrue(normalisedQuery.contains("AND :otherCourthouse IS NULL"));
        Assertions.assertTrue(normalisedQuery.contains("AND :courthouseCode IS NULL"));
        Assertions.assertTrue(normalisedQuery.contains("ORDER BY c.list_date DESC, c.ale_id DESC"));
        Assertions.assertTrue(normalisedQuery.contains("LIMIT :limit"));

        Assertions.assertEquals(-1, normalisedQuery.indexOf(":standardApplicantCode"));
        Assertions.assertEquals(-1, normalisedQuery.indexOf(":applicantOrganisation"));
        Assertions.assertEquals(-1, normalisedQuery.indexOf("latest_fee_status AS"));
        Assertions.assertEquals(-1, normalisedQuery.indexOf("app_list_entry_fee_id"));
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
        return resultSet;
    }
}
