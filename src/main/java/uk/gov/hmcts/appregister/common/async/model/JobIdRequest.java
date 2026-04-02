package uk.gov.hmcts.appregister.common.async.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A job id is a combination of the id and the user name
*/
@EqualsAndHashCode
@Builder
@Getter
public class JobIdRequest {
    String id;
    String userName;
}
