package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;

abstract class AbstractReportDataReader<T, C extends AbstractReportDataReader.ReportReadCursor<T>>
        implements DataReader<T> {
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    private final String schema;
    private final int maxRows;

    AbstractReportDataReader(NamedParameterJdbcTemplate jdbcTemplate, String schema, int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be greater than zero");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
        this.maxRows = maxRows;
    }

    @Override
    public final void readData(
            ReadPagePosition position, PageReader<T> pageReader, JobContext jobContext)
            throws IOException {
        setSearchPath();

        C cursor = createCursor(position.getPageSize());
        int rowsRead = 0;

        while (rowsRead < maxRows) {
            int pageLimit = Math.min(cursor.pageSize(), maxRows - rowsRead);
            List<T> rows = readPage(cursor, pageLimit);
            if (rows.isEmpty()) {
                return;
            }

            pageReader.readData(rows, jobContext);
            rowsRead += rows.size();
            if (rows.size() < pageLimit) {
                return;
            }
            cursor.advance(rows);
        }
    }

    @Override
    public void close() throws IOException {
        // No stream to close.
    }

    protected abstract C createCursor(int pageSize);

    protected abstract List<T> readPage(C cursor, int pageLimit);

    protected static String legacyLocationValue(
            LegacyReportLocation location, Function<LegacyReportLocation, String> getter) {
        return location == null ? null : getter.apply(location);
    }

    private void setSearchPath() {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.
    }

    protected abstract static class ReportReadCursor<T> {
        private final int pageSize;
        private T lastRow;

        ReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        int pageSize() {
            return pageSize;
        }

        void advance(List<T> rows) {
            lastRow = rows.getLast();
        }

        boolean hasLastRow() {
            return lastRow != null;
        }

        T lastRow() {
            return lastRow;
        }
    }
}
