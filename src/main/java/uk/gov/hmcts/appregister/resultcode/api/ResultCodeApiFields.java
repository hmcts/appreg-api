package uk.gov.hmcts.appregister.resultcode.api;

import lombok.experimental.UtilityClass;

/**
 * Defines the API field names exposed by the Result Code endpoints.
 *
 * <p>These constants represent the property names that clients can use in filters, sorting, or
 * query parameters.
 */
@UtilityClass
public class ResultCodeApiFields {
    public static final String CODE = "resultCode";
    public static final String TITLE = "title";
}
