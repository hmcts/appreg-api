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
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;

class PrivateProsecutorsIndexReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<PrivateProsecutorsIndexReportRow>> pages = new ArrayList<>();
        PageReader<PrivateProsecutorsIndexReportRow> pageReader =
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
                        ArgumentMatchers.<RowMapper<PrivateProsecutorsIndexReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<PrivateProsecutorsIndexReportRow> rowMapper =
                                    invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        PrivateProsecutorsIndexReportDataReader reader =
                new PrivateProsecutorsIndexReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        PrivateProsecutorsIndexReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(123L, row.getApplicationListEntryId());
        Assertions.assertEquals(LocalDate.of(2018, 5, 18), row.getListDate());
        Assertions.assertEquals("B01IX00 - Westminster", row.getCourthouseName());
        Assertions.assertEquals("Other court", row.getOtherCourthouse());
        Assertions.assertEquals("01", row.getCjaCode());
        Assertions.assertEquals("Smith", row.getApplicantNameOrSurname());
        Assertions.assertEquals("John", row.getApplicantFirstName());
        Assertions.assertEquals("Standard applicant", row.getStandardApplicantName());
        Assertions.assertEquals("Jane", row.getRespondentFirstName());
        Assertions.assertEquals("Bloggs", row.getRespondentSurname());
        Assertions.assertEquals("Widgets Ltd", row.getRespondentOrganisationName());
        Assertions.assertEquals("Wording", row.getApplicationWording());
        Assertions.assertEquals("R4", row.getResult1());
        Assertions.assertEquals("R3", row.getResult2());
        Assertions.assertEquals("R2", row.getResult3());
        Assertions.assertEquals("R1", row.getResult4());
        Assertions.assertEquals("Notes", row.getNotes());

        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertLegacyPrivateProsecutorsIndexQueryShape(queries.getFirst());
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
                        ArgumentMatchers.<RowMapper<PrivateProsecutorsIndexReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            return List.of();
                        });

        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));
        PrivateProsecutorsIndexReportDataReader reader =
                new PrivateProsecutorsIndexReportDataReader(jdbcTemplate, filter, "appreg");
        PageReader<PrivateProsecutorsIndexReportRow> pageReader =
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
                        ArgumentMatchers.<RowMapper<PrivateProsecutorsIndexReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<PrivateProsecutorsIndexReportRow> rowMapper =
                                    invocation.getArgument(2);
                            return List.of(rowMapper.mapRow(resultSet(), 0));
                        });

        PrivateProsecutorsIndexReportDataReader reader =
                new PrivateProsecutorsIndexReportDataReader(jdbcTemplate, filter(), "appreg");

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
        Assertions.assertEquals("Smith", parameters.getValue("applicantSurname"));
        Assertions.assertEquals("John", parameters.getValue("applicantFirstName"));
        Assertions.assertEquals("Acme", parameters.getValue("applicantOrganisationName"));
        Assertions.assertEquals("CPS", parameters.getValue("standardApplicantName"));
        Assertions.assertEquals("Bloggs", parameters.getValue("respondentSurname"));
        Assertions.assertEquals("Jane", parameters.getValue("respondentFirstName"));
        Assertions.assertEquals("Widgets", parameters.getValue("respondentOrganisationName"));
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

    private PrivateProsecutorsIndexFilterDto filter() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .cjaCode("01")
                        .otherLocationDescription("Other court")
                        .courtLocationCode("B01IX00");
        return new PrivateProsecutorsIndexFilterDto()
                .dateFrom(LocalDate.of(2018, 5, 1))
                .dateTo(LocalDate.of(2018, 5, 31))
                .applicantSurname("Smith")
                .applicantFirstName("John")
                .applicantOrganisationName("Acme")
                .standardApplicantName("CPS")
                .respondentSurname("Bloggs")
                .respondentFirstName("Jane")
                .respondentOrganisationName("Widgets")
                .location(location);
    }

    private void assertLegacyPrivateProsecutorsIndexQueryShape(String query) {
        Assertions.assertTrue(query.contains("JOIN name_address app_na"));
        Assertions.assertTrue(query.contains("FROM standard_applicants sa"));
        Assertions.assertTrue(query.contains("NULLIF(TRIM(sa.name), '')"));
        Assertions.assertTrue(query.contains("app_na.first_name"));
        Assertions.assertTrue(query.contains("app_na.last_name"));
        Assertions.assertTrue(query.contains("COALESCE(sa.forename_1, '')"));
        Assertions.assertTrue(query.contains("COALESCE(sa.surname, '')"));
        Assertions.assertTrue(query.contains("sa.standard_applicant_code"));
        Assertions.assertTrue(query.contains("AND ale.sa_sa_id IS NULL"));
        Assertions.assertTrue(query.contains("OR UPPER(sa.standard_applicant_name)"));
        Assertions.assertTrue(query.contains("al.application_list_status = 'CLOSED'"));
        Assertions.assertTrue(query.contains("ac.application_code = 'MX99010'"));
        Assertions.assertTrue(query.contains("al.is_deleted IS NULL OR al.is_deleted <> 'Y'"));
        Assertions.assertTrue(query.contains("ale.is_deleted IS NULL OR ale.is_deleted <> 'Y'"));
        Assertions.assertTrue(query.contains("SUBSTRING(al.courthouse_code FROM 2 FOR 2)"));
        Assertions.assertTrue(query.contains("ROW_NUMBER() OVER"));
        Assertions.assertTrue(query.contains("ORDER BY rc.resolution_code DESC"));
        Assertions.assertTrue(
                query.contains("ORDER BY fa.application_list_date DESC, fa.ale_id DESC"));
    }

    private ResultSet resultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("ale_id")).thenReturn(123L);
        when(resultSet.getObject("list_date", LocalDate.class))
                .thenReturn(LocalDate.of(2018, 5, 18));
        when(resultSet.getString("courthouse_name")).thenReturn("B01IX00 - Westminster");
        when(resultSet.getString("other_courthouse")).thenReturn("Other court");
        when(resultSet.getString("cja_code")).thenReturn("01");
        when(resultSet.getString("applicant_name_or_surname")).thenReturn("Smith");
        when(resultSet.getString("applicant_first_name")).thenReturn("John");
        when(resultSet.getString("standard_applicant_name")).thenReturn("Standard applicant");
        when(resultSet.getString("respondent_first_name")).thenReturn("Jane");
        when(resultSet.getString("respondent_surname")).thenReturn("Bloggs");
        when(resultSet.getString("respondent_organisation_name")).thenReturn("Widgets Ltd");
        when(resultSet.getString("application_wording")).thenReturn("Wording");
        when(resultSet.getString("result1")).thenReturn("R4");
        when(resultSet.getString("result2")).thenReturn("R3");
        when(resultSet.getString("result3")).thenReturn("R2");
        when(resultSet.getString("result4")).thenReturn("R1");
        when(resultSet.getString("notes")).thenReturn("Notes");
        return resultSet;
    }
}
