package uk.gov.hmcts.appregister.testutils;

import io.restassured.specification.RequestSpecification;
import java.util.Optional;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.enumeration.Status;

/**
 * A rest assured request specification that knows what filters can be applied to get specific
 * application list.
 */
@RequiredArgsConstructor
@Builder
public class GetApplicationListFilterSpecification implements UnaryOperator<RequestSpecification> {

    // Need to be in HH:MM format
    @Builder.Default private final Optional<String> localTime = Optional.empty();

    // Need to be in YYYY-MM-DD format
    @Builder.Default private final Optional<String> dateValue = Optional.empty();

    @Builder.Default private final Optional<String> courtLocationCode = Optional.empty();

    @Builder.Default private final Optional<String> cjaCode = Optional.empty();

    @Builder.Default private final Optional<String> description = Optional.empty();

    @Builder.Default private final Optional<String> otherLocationDescription = Optional.empty();

    @Builder.Default private final Optional<Status> status = Optional.empty();

    @Override
    public RequestSpecification apply(RequestSpecification rs) {
        if (dateValue.isPresent()) {
            rs = rs.queryParam("date", dateValue.get());
        }

        if (localTime.isPresent()) {
            rs = rs.queryParam("time", localTime.get());
        }

        if (courtLocationCode.isPresent()) {
            rs = rs.queryParam("courtLocationCode", courtLocationCode.get());
        }

        if (cjaCode.isPresent()) {
            rs = rs.queryParam("cjaCode", cjaCode.get());
        }

        if (description.isPresent()) {
            rs = rs.queryParam("description", description.get());
        }

        if (otherLocationDescription.isPresent()) {
            rs = rs.queryParam("otherLocationDescription", otherLocationDescription.get());
        }

        if (status.isPresent()) {
            rs = rs.queryParam("status", status.get());
        }

        return rs;
    }
}
