package uk.gov.hmcts.appregister.dto.read;

import java.time.LocalDate;

public record CourtHouseDto(
        Long id,
        String name,
        String courtType,
        LocalDate startDate,
        LocalDate endDate,
        Long locationId,
        Long psaId,
        String courtLocationCode,
        String welshName,
        Long orgId) {}
