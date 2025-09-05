package uk.gov.hmcts.appregister.applicationentry.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.dto.CsvRowDto;

/** Parser for CSV files with pipe (|) delimiter. */
@Component
public class CsvParser implements Parser<CsvRowDto> {

  private static final int COLUMN_COUNT = 20;

  @Override
  public List<CsvRowDto> parse(MultipartFile file) {
    List<CsvRowDto> result = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
      String line;
      boolean isHeader = true;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;

        // Skip header
        if (isHeader) {
          isHeader = false;
          continue;
        }

        if (line.trim().isEmpty()) {
          continue; // Skip blank lines
        }

        CsvRowDto row = getCsvRowDto(line, lineNumber);
        result.add(row);
      }

    } catch (IOException e) {
      throw new UncheckedIOException("Could not read file", e);
    }

    return result;
  }

  private CsvRowDto getCsvRowDto(String line, int lineNumber) {
    String[] parts = line.split("\\|", -1); // -1 to keep trailing empty strings

    if (parts.length != COLUMN_COUNT) {
      throw new IllegalArgumentException(
          "Line " + lineNumber + " has " + parts.length + " columns; expected " + COLUMN_COUNT);
    }

    return new CsvRowDto(
        trim(parts[0]),
        trim(parts[1]),
        trim(parts[2]),
        trim(parts[3]),
        trim(parts[4]),
        trim(parts[5]),
        trim(parts[6]),
        trim(parts[7]),
        trim(parts[8]),
        trim(parts[9]),
        trim(parts[10]),
        trim(parts[11]),
        trim(parts[12]),
        trim(parts[13]),
        trim(parts[14]),
        trim(parts[15]),
        trim(parts[16]),
        trim(parts[17]),
        trim(parts[18]),
        trim(parts[19]));
  }

  private String trim(String input) {
    return input == null ? null : input.trim();
  }
}
