package uk.gov.hmcts.appregister.report.service;

final class LegacyMisReportLocationSql {
    private LegacyMisReportLocationSql() {
        // Utility class.
    }

    static String predicate(
            String courthouseCodeExpression,
            String otherCourthouseExpression,
            String cjaCodeExpression,
            String otherLocationParameterName) {
        return """
                (
                    :cjaCode IS NOT NULL
                    AND UPPER(%3$s) = UPPER(:cjaCode)
                    AND UPPER(%2$s)
                        LIKE '%%' || UPPER(:%4$s) || '%%'
                    AND :courthouseCode IS NULL
                )
                OR (
                    :cjaCode IS NULL
                    AND (
                        UPPER(%2$s)
                            LIKE '%%' || UPPER(:%4$s) || '%%'
                        OR :%4$s IS NULL
                    )
                    AND (
                        UPPER(%1$s)
                            LIKE '%%' || UPPER(:courthouseCode) || '%%'
                        OR :courthouseCode IS NULL
                    )
                )
                OR (
                    :cjaCode IS NOT NULL
                    AND (
                        UPPER(SUBSTRING(%1$s FROM 2 FOR 2))
                            = UPPER(:cjaCode)
                        OR UPPER(%3$s) = UPPER(:cjaCode)
                    )
                    AND :%4$s IS NULL
                    AND :courthouseCode IS NULL
                )
                OR (
                    :cjaCode IS NULL
                    AND :%4$s IS NULL
                    AND :courthouseCode IS NULL
                )
                """
                .formatted(
                        courthouseCodeExpression,
                        otherCourthouseExpression,
                        cjaCodeExpression,
                        otherLocationParameterName);
    }
}
