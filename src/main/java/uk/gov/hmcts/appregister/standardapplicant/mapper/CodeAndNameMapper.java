package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;

public record CodeAndNameMapper(
        String code, String name, String addressLine1, LocalDate from, LocalDate to) {}
