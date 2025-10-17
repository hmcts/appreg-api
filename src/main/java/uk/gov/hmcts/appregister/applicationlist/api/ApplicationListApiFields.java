package uk.gov.hmcts.appregister.applicationlist.api;

/**
 * Defines the API field names exposed by the Application List endpoints.
 *
 * <p>These constants represent the property names that clients can use in filters, sorting, or
 * query parameters.
 */
public final class ApplicationListApiFields {
    private ApplicationListApiFields() {
        // Utility class
    }

    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final String STATUS = "status";
    public static final String COURT_LOCATION_CODE = "courtLocationCode";
    public static final String CJA = "cja";
    public static final String DESCRIPTION = "description";
    public static final String OTHER_LOCATION_DESCRIPTION = "otherLocationDescription";
}
