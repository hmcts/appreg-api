package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;

public record CodeAndName(
        String code, String name, String addressLine1, LocalDate from, LocalDate to) {}
