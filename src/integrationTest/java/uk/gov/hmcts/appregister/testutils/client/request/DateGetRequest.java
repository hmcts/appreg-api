package uk.gov.hmcts.appregister.testutils.client.request;

import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

/**
 * A request specification that applies a date get parameter to the request.
 */
@RequiredArgsConstructor
public class DateGetRequest implements UnaryOperator<RequestSpecification> {
    private final LocalDate date;

    @Override
    public RequestSpecification apply(RequestSpecification rs) {
        rs = rs.queryParam("date", date.toString());
        return rs;
    }
}