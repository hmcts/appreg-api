package uk.gov.hmcts.appregister.applicationentryresult.model;

import java.util.UUID;

public record ListEntryResultDeleteArgs(UUID listId, UUID entryId, UUID resultId) {}
