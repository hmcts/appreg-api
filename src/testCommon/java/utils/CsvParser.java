package utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.StringReader;
import java.util.List;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;

public class CsvParser {
    public static List<StandardApplicantCsvRow> parseCsv(String csv) {
        StringReader stringReader = new StringReader(csv);
        ColumnPositionMappingStrategy<StandardApplicantCsvRow> mappingStrategy =
                new ColumnPositionMappingStrategy<>();
        mappingStrategy.setType(StandardApplicantCsvRow.class);

        CSVParser csvParser = new CSVParserBuilder().withSeparator('|').build();
        CSVReader csvReader = new CSVReaderBuilder(stringReader).withCSVParser(csvParser).build();

        CsvToBean<StandardApplicantCsvRow> reader =
                new CsvToBeanBuilder<StandardApplicantCsvRow>(csvReader)
                        .withType(StandardApplicantCsvRow.class)
                        .build();
        return reader.parse();
    }
}
