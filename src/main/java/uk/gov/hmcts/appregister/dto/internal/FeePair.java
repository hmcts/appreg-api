package uk.gov.hmcts.appregister.dto.internal;

import uk.gov.hmcts.appregister.model.ApplicationFee;

public record FeePair(ApplicationFee mainFee, ApplicationFee offsetFee) {}
