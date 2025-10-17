package uk.gov.hmcts.appregister.applicationlist.mapper;

import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListApiFields;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListSortValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Maps and validates API sort parameters for Application List queries.
 *
 * <p>Acts as the bridge between external API field names and internal entity properties, enforcing
 * allowed fields and sort directions. Delegates field validation to {@link
 * ApplicationListSortValidator}.
 */
@Component
@RequiredArgsConstructor
public class ApplicationListSortMapper {

    private static final String ASC = "asc";
    private static final String DESC = "desc";
    private static final int MAX_SORT_VAL = 2;
    private static final int API_FIELD_INDEX = 0;
    private static final int DIRECTION_INDEX = 1;
    private static final String SORT_DELIMITER = ",";

    /*
     * A map which links the API field name to the entity property name.
     */
    private static final Map<String, String> SORT_MAP =
            Map.of(
                    ApplicationListApiFields.DATE, ApplicationList_.DATE,
                    ApplicationListApiFields.TIME, ApplicationList_.TIME,
                    ApplicationListApiFields.STATUS, ApplicationList_.STATUS,
                    ApplicationListApiFields.COURT_LOCATION_CODE, ApplicationList_.COURT_CODE,
                    ApplicationListApiFields.CJA, ApplicationList_.CJA,
                    ApplicationListApiFields.DESCRIPTION, ApplicationList_.DESCRIPTION,
                    ApplicationListApiFields.OTHER_LOCATION_DESCRIPTION,
                            ApplicationList_.OTHER_LOCATION);

    private final ApplicationListSortValidator sortValidator;

    /**
     * Translates API sort fields (e.g. "date,desc") into validated entity property names used for
     * persistence queries. If the client provides no sort fields, an empty list is returned so that
     * the caller can apply default sorting behavior.
     *
     * @param sorts e.g. ["date,desc","status,asc"]
     * @return e.g. ["listDate,desc","status,asc"]
     */
    public List<String> mapAndValidate(List<String> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> mappedSorts = new ArrayList<>();
        for (String sortValue : sorts) {
            if (sortValue == null || sortValue.isBlank()) {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE, "Sort value is blank");
            }

            String[] sortParts = sortValue.split(SORT_DELIMITER, MAX_SORT_VAL);
            String apiField = sortParts[API_FIELD_INDEX].trim();

            String entityField = SORT_MAP.get(apiField);
            if (entityField == null) {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE,
                        "Sort property '%s' is not allowed. Allowed: %s"
                                .formatted(apiField, String.join(", ", SORT_MAP.keySet())));
            }

            sortValidator.validate(entityField);

            boolean hasDirection = sortParts.length > 1;
            if (hasDirection) {
                String direction = checkDirection(sortParts, apiField);
                mappedSorts.add(entityField + SORT_DELIMITER + direction);
            } else {
                mappedSorts.add(entityField);
            }
        }
        return mappedSorts;
    }

    private static String checkDirection(String[] sortParts, String apiField) {
        String direction = sortParts[DIRECTION_INDEX].trim();
        String norm = direction.toLowerCase(ROOT);
        if (!norm.equals(ASC) && !norm.equals(DESC)) {
            throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE,
                    "Sort direction '%s' is not valid for property '%s'. Use 'asc' or 'desc'."
                            .formatted(direction, apiField));
        }
        return norm;
    }
}
