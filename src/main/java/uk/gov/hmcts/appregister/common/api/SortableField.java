package uk.gov.hmcts.appregister.common.api;

import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * A sortable field that contains information to parse a sort string from the API e.g. field,ASC. It
 * also contains the ability to contain an equivalent string representation for the entity field
 * using a lookup function.
 */
@Getter
@RequiredArgsConstructor
public class SortableField {
    private final String field;
    private final String direction;

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    private static final int API_FIELD_INDEX = 0;
    private static final int DIRECTION_INDEX = 1;

    private static final String SORT_DELIMITER = ",";

    private static final int MAX_SORT_VAL = 2;

    /**
     * Parses the provided fields into a list of SortableField objects.
     *
     * @param fields the fields to parse
     * @return The parsed repreresentation of the sortable fields
     */
    public static List<SortableField> of(String... fields) {
        List<SortableField> sortableFields = new ArrayList<>();
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new AppRegistryException(
                        CommonAppError.SORT_NOT_SUITABLE, "Sort value is blank");
            }

            String[] sortParts = field.split(SORT_DELIMITER, MAX_SORT_VAL);
            String apiField = sortParts[API_FIELD_INDEX].trim();

            boolean hasDirection = sortParts.length > 1;
            String direction = null;
            if (hasDirection) {
                direction = checkDirection(sortParts, apiField);
            }

            sortableFields.add(new SortableField(apiField, direction));
        }

        return sortableFields;
    }

    /**
     * Maps the sortable field to the associated entity field using a lookup function..
     *
     * @param lookup function to map API field names to SortableFieldsEnum
     * @return The string mapping the sortable field in the format "entityField,direction"
     */
    public String toSortStringUsingSortableOperation(
            Function<String, SortableOperationEnum> lookup) {
        SortableOperationEnum sortableField = lookup.apply(this.field);
        if (sortableField == null) {
            throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE,
                    "Sort property '%s' is not allowed.".formatted(this.field));
        }

        if (direction != null) {
            return lookup.apply(field).getEntityValue() + SORT_DELIMITER + direction;
        } else {
            return lookup.apply(field).getEntityValue();
        }
    }

    private static String checkDirection(String[] sortParts, String apiField) {
        String direction = sortParts[DIRECTION_INDEX].trim();
        String norm = direction.toLowerCase(ROOT);
        if (!norm.equals(ASC) && !norm.equals(DESC)) {
            throw new AppRegistryException(
                    CommonAppError.SORT_DIRECTION_NOT_SUITABLE,
                    "Sort direction '%s' is not valid for property '%s'. Use 'asc' or 'desc'."
                            .formatted(direction, apiField));
        }
        return norm;
    }
}
