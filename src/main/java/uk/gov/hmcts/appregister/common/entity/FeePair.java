package uk.gov.hmcts.appregister.common.entity;

/** A record to hold a pair of fees: the main fee and an optional offset fee. */
public record FeePair(Fee mainFee, Fee offsetFee) {}
