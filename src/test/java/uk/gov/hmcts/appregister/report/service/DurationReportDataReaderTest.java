package uk.gov.hmcts.appregister.report.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<DurationReportRow>> pages = new ArrayList<>();
        PageReader<DurationReportRow> pageReader =
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
                        ArgumentMatchers.<RowMapper<DurationReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<DurationReportRow> rowMapper = invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        DurationReportDataReader reader =
                new DurationReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        DurationReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(123L, row.getApplicationListId());
        Assertions.assertEquals(LocalDate.of(2018, 5, 18), row.getListDate());
        Assertions.assertEquals("B01IX00 - Westminster", row.getCourthouseName());
        Assertions.assertEquals("Other court", row.getOtherCourthouse());
        Assertions.assertEquals("01", row.getCjaCode());
        Assertions.assertEquals("Morning list", row.getListDescription());
        Assertions.assertEquals(2, row.getDurationHours());
        Assertions.assertEquals(45, row.getDurationMinutes());

        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertLegacyDurationQueryShape(queries.getFirst());
    }

    @Test
    void givenNoLocation_whenReadData_thenLocationParametersAreNull() throws IOException {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<DurationReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            return List.of();
                        });

        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));
        DurationReportDataReader reader =
                new DurationReportDataReader(jdbcTemplate, filter, "appreg");
        PageReader<DurationReportRow> pageReader =
                (rows, context) -> Assertions.fail("No rows expected");

        reader.readData(new ReadPagePosition(25, 5), pageReader, mock(JobContext.class));

        MapSqlParameterSource parameters = parameterSources.getFirst();
        Assertions.assertNull(parameters.getValue("cjaCode"));
        Assertions.assertNull(parameters.getValue("otherCourthouse"));
        Assertions.assertNull(parameters.getValue("courthouseCode"));
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
                        ArgumentMatchers.<RowMapper<DurationReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<DurationReportRow> rowMapper = invocation.getArgument(2);
                            return List.of(rowMapper.mapRow(resultSet(), 0));
                        });

        DurationReportDataReader reader =
                new DurationReportDataReader(jdbcTemplate, filter(), "appreg");

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
            Assertions.assertEquals(123L, parameters.getValue("lastApplicationListId"));
        } else {
            Assertions.assertNull(parameters.getValue("lastListDate"));
            Assertions.assertNull(parameters.getValue("lastApplicationListId"));
        }
    }

    private DurationFilterDto filter() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .cjaCode("01")
                        .otherLocationDescription("Other court")
                        .courtLocationCode("B01IX00");
        return new DurationFilterDto()
                .dateFrom(LocalDate.of(2018, 5, 1))
                .dateTo(LocalDate.of(2018, 5, 31))
                .location(location);
    }

    private void assertLegacyDurationQueryShape(String query) {
        assertThat(query).contains("FROM application_lists al");
        assertThat(query).contains("LEFT JOIN criminal_justice_area cja");
        assertThat(query).contains("al.application_list_status = 'CLOSED'");
        assertThat(query).contains("al.is_deleted IS NULL OR al.is_deleted <> 'Y'");
        assertThat(query).contains("SUBSTRING(al.courthouse_code FROM 2 FOR 2)");
        Assertions.assertTrue(
                query.contains("ORDER BY al.application_list_date DESC, al.al_id DESC"));
    }

    private ResultSet resultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("al_id")).thenReturn(123L);
        when(resultSet.getObject("list_date", LocalDate.class))
                .thenReturn(LocalDate.of(2018, 5, 18));
        when(resultSet.getString("courthouse_name")).thenReturn("B01IX00 - Westminster");
        when(resultSet.getString("other_courthouse")).thenReturn("Other court");
        when(resultSet.getString("cja_code")).thenReturn("01");
        when(resultSet.getString("list_description")).thenReturn("Morning list");
        when(resultSet.getObject("duration_hour")).thenReturn(2);
        when(resultSet.getObject("duration_minute")).thenReturn(45);
        return resultSet;
    }
}
