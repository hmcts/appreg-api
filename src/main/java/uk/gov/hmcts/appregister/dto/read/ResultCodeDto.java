package uk.gov.hmcts.appregister.dto.read;

import java.time.LocalDate;

public record ResultCodeDto(
    Long id,
    String resultCode,
    String title,
    String wording,
    String legislation,
    String destinationEmail1,
    String destinationEmail2,
    LocalDate startDate,
    LocalDate endDate
) {}
