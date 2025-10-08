package uk.gov.hmcts.appregister.applicationlist.mapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListApiFields;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListSortValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Locale.ROOT;

@Component
@RequiredArgsConstructor
public class ApplicationListSortMapper {

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    private static final Map<String, String> SORT_MAP = Map.of(
        ApplicationListApiFields.DATE, ApplicationList_.DATE,
        ApplicationListApiFields.TIME, ApplicationList_.TIME,
        ApplicationListApiFields.STATUS, ApplicationList_.STATUS,
        ApplicationListApiFields.COURT_LOCATION_CODE, ApplicationList_.COURT_CODE,
        ApplicationListApiFields.CJA, ApplicationList_.CJA,
        ApplicationListApiFields.DESCRIPTION, ApplicationList_.DESCRIPTION,
        ApplicationListApiFields.OTHER_LOCATION_DESCRIPTION, ApplicationList_.OTHER_LOCATION
    );

    private final ApplicationListSortValidator sortValidator;

    /**
     * Map API sort tokens to entity-property tokens and validate PROPERTY names.
     * Returns an empty list if client supplied no sort (so caller can apply defaults).
     *
     * @param sorts e.g. ["date,desc","status,asc"]
     * @return      e.g. ["listDate,desc","status,asc"]
     */
    public List<String> mapAndValidate(List<String> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> mapped = new ArrayList<>();
        for (String sortValue : sorts) {
            if (sortValue == null || sortValue.isBlank()) {
                throw new AppRegistryException(CommonAppError.SORT_NOT_SUITABLE, "Sort value is blank");
            }

            String[] parts = sortValue.split(",", 2);
            String apiField = parts[0].trim();

            // Validate API field exists in mapping
            String entityField = SORT_MAP.get(apiField);
            if (entityField == null) {
                throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE,
                    "Sort property '%s' is not allowed. Allowed: %s"
                        .formatted(apiField, String.join(", ", SORT_MAP.keySet())));
            }

            // Validate the ENTITY PROPERTY via the domain validator
            sortValidator.validate(entityField);

            if (parts.length > 1) {
                String direction = checkDirection(parts, apiField);
                mapped.add(entityField + "," + direction);
            } else {
                mapped.add(entityField); // no direction → let downstream default to ASC
            }
        }
        return mapped;
    }

    private static String checkDirection(String[] parts, String apiField) {
        String direction = parts[1].trim();
        // Strict direction check (asc|desc), case-insensitive
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
