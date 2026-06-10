package uk.gov.hmcts.appregister.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
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
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

class WorkloadReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<WorkloadReportRow>> pages = new ArrayList<>();
        PageReader<WorkloadReportRow> pageReader =
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
                        ArgumentMatchers.<RowMapper<WorkloadReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<WorkloadReportRow> rowMapper = invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        WorkloadFilterDto filter = filter();
        WorkloadReportDataReader reader =
                new WorkloadReportDataReader(jdbcTemplate, filter, "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        WorkloadReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 18), row.getListDate());
        Assertions.assertEquals("B01IX00 - Test Court", row.getListCourtHouseName());
        Assertions.assertEquals("Other court", row.getListOtherLocation());
        Assertions.assertEquals("British Gas", row.getApplicantNameSurname());
        Assertions.assertEquals("RE99001", row.getApplicationCode());
        Assertions.assertEquals("Rights of Entry Warrant", row.getApplicationCodeTitle());

        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertLegacyWorkloadsQueryShape(queries.getFirst());
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
                        ArgumentMatchers.<RowMapper<WorkloadReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            return List.of();
                        });

        WorkloadFilterDto filter =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31));
        WorkloadReportDataReader reader =
                new WorkloadReportDataReader(jdbcTemplate, filter, "appreg");
        PageReader<WorkloadReportRow> pageReader =
                (rows, context) -> Assertions.fail("No rows expected");

        reader.readData(new ReadPagePosition(25, 5), pageReader, mock(JobContext.class));

        MapSqlParameterSource parameters = parameterSources.getFirst();
        Assertions.assertNull(parameters.getValue("cjaCode"));
        Assertions.assertNull(parameters.getValue("otherLocation"));
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
                        ArgumentMatchers.<RowMapper<WorkloadReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<WorkloadReportRow> rowMapper = invocation.getArgument(2);
                            return List.of(rowMapper.mapRow(resultSet(), 0));
                        });

        WorkloadReportDataReader reader =
                new WorkloadReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(
                new ReadPagePosition(25, 0),
                (rows, context) -> Assertions.assertEquals(1, rows.size()),
                mock(JobContext.class));

        Assertions.assertEquals(1, parameterSources.size());
        Assertions.assertEquals(25, parameterSources.getFirst().getValue("limit"));
    }

    private void assertParameters(MapSqlParameterSource parameters, boolean expectedCursor) {
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), parameters.getValue("dateFrom"));
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), parameters.getValue("dateTo"));
        Assertions.assertEquals("01", parameters.getValue("cjaCode"));
        Assertions.assertEquals("Other court", parameters.getValue("otherLocation"));
        Assertions.assertEquals("B01IX00", parameters.getValue("courthouseCode"));
        Assertions.assertEquals(1, parameters.getValue("limit"));
        Assertions.assertEquals(expectedCursor, parameters.getValue("hasCursor"));

        if (expectedCursor) {
            Assertions.assertEquals(
                    LocalDate.of(2018, Month.MAY, 18), parameters.getValue("lastListDate"));
        } else {
            Assertions.assertNull(parameters.getValue("lastListDate"));
        }
    }

    private WorkloadFilterDto filter() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .cjaCode("01")
                        .otherLocationDescription("Other court")
                        .courtLocationCode("B01IX00");
        return new WorkloadFilterDto()
                .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                .dateTo(LocalDate.of(2018, Month.MAY, 31))
                .location(location);
    }

    private void assertLegacyWorkloadsQueryShape(String query) {
        String normalisedQuery = query.replaceAll("\\s+", " ");
        assertThat(normalisedQuery).contains("first_name as forename_1");
        assertThat(normalisedQuery).contains("middle_name as forename_2");
        assertThat(normalisedQuery).contains("null as forename_3");
        assertThat(normalisedQuery).contains("last_name as surname");
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(al.other_courthouse) "
                                + "LIKE '%' || UPPER(:otherLocation) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains(
                        "UPPER(al.courthouse_code) "
                                + "LIKE '%' || UPPER(:courthouseCode) || '%'"));
        Assertions.assertTrue(
                normalisedQuery.contains("UPPER(cja.cja_code) LIKE '%' || UPPER(:cjaCode) || '%'"));
    }

    private ResultSet resultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getDate("list_date")).thenReturn(Date.valueOf("2018-05-18"));
        when(resultSet.getString("courthouse_name")).thenReturn("B01IX00 - Test Court");
        when(resultSet.getString("list_other_location")).thenReturn("Other court");
        when(resultSet.getString("applicant_name")).thenReturn("British Gas");
        when(resultSet.getString("application_code")).thenReturn("RE99001");
        when(resultSet.getString("application_code_title")).thenReturn("Rights of Entry Warrant");
        when(resultSet.getString("official")).thenReturn("Test official");
        when(resultSet.getString("jp1")).thenReturn("Test JP1");
        when(resultSet.getString("jp2")).thenReturn("Test JP2");
        when(resultSet.getString("jp3")).thenReturn("Test JP3");
        when(resultSet.getString("resolution_codes")).thenReturn("A1,B2,C3");
        return resultSet;
    }
}
